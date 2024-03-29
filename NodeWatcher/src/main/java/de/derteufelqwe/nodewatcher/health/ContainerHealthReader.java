package de.derteufelqwe.nodewatcher.health;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.HealthState;
import com.github.dockerjava.api.command.HealthStateLog;
import com.github.dockerjava.api.command.InspectContainerResponse;
import com.github.dockerjava.api.exception.NotFoundException;
import de.derteufelqwe.commons.CommonsAPI;
import de.derteufelqwe.commons.hibernate.SessionBuilder;
import de.derteufelqwe.commons.hibernate.objects.DBContainer;
import de.derteufelqwe.commons.hibernate.objects.DBContainerHealth;
import de.derteufelqwe.commons.misc.RepeatingThread;
import de.derteufelqwe.nodewatcher.NodeWatcher;
import de.derteufelqwe.nodewatcher.exceptions.DBContainerNotFoundException;
import de.derteufelqwe.nodewatcher.executors.ContainerEventHandler;
import de.derteufelqwe.nodewatcher.misc.IContainerObserver;
import de.derteufelqwe.nodewatcher.misc.NWUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.sql.Timestamp;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


/**
 * Responsible for periodically downloading the new logs for a container.
 * Containers are added by {@link ContainerEventHandler}
 */
public class ContainerHealthReader extends RepeatingThread implements IContainerObserver {

    // 'DOTALL' flag is required so the regex can match over multiple lines
    @Deprecated
    private final Pattern RE_CLEAN_CURL = Pattern.compile("(.+)?% Total +% Received +% Xferd +Average +Speed +Time +Time +Time +Current.+(curl:.+)", Pattern.DOTALL);
    private final Pattern RE_STRIP_MSG = Pattern.compile("^[ \\t\\n]+|[ \\t\\n]+$");

    private Logger logger = LogManager.getLogger(getClass().getName());
    private final SessionBuilder sessionBuilder = NodeWatcher.getSessionBuilder();
    private final DockerClient dockerClient = NodeWatcher.getDockerClientFactory().forceNewDockerClient();

    private AtomicBoolean doRun = new AtomicBoolean(true);
    private final Set<String> runningContainers = new HashSet<>();


    public ContainerHealthReader() {
        super(10000);
    }


    @Override
    public void onNewContainer(String containerId) {
        synchronized (this.runningContainers) {
            this.runningContainers.add(containerId);
        }
    }

    @Override
    public void onRemoveContainer(String containerId) {
        synchronized (this.runningContainers) {
            this.runningContainers.remove(containerId);
        }

        // Do one last round to make sure even the last health log was fetched
        this.fetchContainerHealth(containerId);
    }

    @Override
    public void repeatedRun() {
        synchronized (this.runningContainers) {
            logger.debug("Updating healths for {} containers.", this.runningContainers.size());
            for (String id : new HashSet<>(this.runningContainers)) {
                try {
                    this.fetchContainerHealth(id);

                    // Container not found in DB
                } catch (DBContainerNotFoundException e1) {
                    logger.error(e1.getMessage());
                    this.runningContainers.remove(id);

                    // Container not found on host
                } catch (NotFoundException e2) {
                    logger.error("Container {} not found on host.", id);
                    this.runningContainers.remove(id);
                }

            }
        }
    }

    @Override
    public void onException(Exception e) {
        logger.error("Caught exception: {}.", e.getMessage());
        e.printStackTrace(System.err);
        CommonsAPI.getInstance().createExceptionNotification(sessionBuilder, e, NodeWatcher.getMetaData());
    }


    public void interrupt() {
        this.doRun.set(false);
    }

    private void fetchContainerHealth(String containerID) {
        try {
            InspectContainerResponse response = dockerClient.inspectContainerCmd(containerID).exec();
            HealthState healthState = response.getState().getHealth();
            if (healthState == null) {  // Container not started yet
                return;
            }

            sessionBuilder.execute(session -> {
                DBContainer container = session.get(DBContainer.class, containerID);

                for (HealthStateLog log : healthState.getLog()) {
                    Timestamp timestamp = NWUtils.parseTimezonedTimestamp(log.getStart());

                    // Only add health logs, which are newer than the latest logs
                    if (container.getContainerHealths() != null && container.getContainerHealths().size() > 0) {
                        Timestamp oldTimestamp = container.getContainerHealths().get(0).getTimestamp();
                        if (oldTimestamp.equals(timestamp) || oldTimestamp.after(timestamp)) {
                            continue;
                        }
                    }

                    DBContainerHealth health = new DBContainerHealth(
                            timestamp,
                            container,
                            this.cleanLogMessage(log.getOutput()),
                            (short) ((long) log.getExitCodeLong())
                    );
                    session.persist(health);
                }
            });

        } catch (NotFoundException e) {
            // Container not found on the host maybe because its task is long finished and it only needs to be finished in the DB
        }
    }

    /**
     * Used to clean common error log messages.
     *
     * @param message Message to stream.
     * @return
     */
    private String cleanLogMessage(String message) {
//        Matcher m1 = RE_CLEAN_CURL.matcher(message);
//        // Remove irrelevant curl text
//        if (m1.matches()) {
//            message = m1.group(2);
//        }

        Matcher m2 = RE_STRIP_MSG.matcher(message);
        // Strip the string
        if (m2.find()) {
            message = m2.replaceAll("");
        }

        return message;
    }

}
