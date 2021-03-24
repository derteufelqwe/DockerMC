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
import de.derteufelqwe.nodewatcher.NodeWatcher;
import de.derteufelqwe.nodewatcher.exceptions.DBContainerNotFoundException;
import de.derteufelqwe.nodewatcher.executors.ContainerWatcher;

import de.derteufelqwe.nodewatcher.logs.LogLoadCallback;
import de.derteufelqwe.nodewatcher.misc.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.hibernate.Session;
import org.hibernate.Transaction;

import java.sql.Timestamp;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


/**
 * Responsible for periodically downloading the new logs for a container.
 * Containers are added by {@link ContainerWatcher} and removed by {@link LogLoadCallback}
 */
public class ContainerHealthReader extends Thread implements INewContainerObserver, IRemoveContainerObserver {

    private final Pattern RE_CLEAN_CURL = Pattern.compile("(.+% Total +% Received +% Xferd +Average +Speed +Time +Time +Time +Current .+curl: \\(\\d+\\) )(.+)");
    private final int FETCH_INTERVAL = 12;  // in seconds

    private Logger logger = LogManager.getLogger(getClass().getName());
    private final SessionBuilder sessionBuilder = NodeWatcher.getSessionBuilder();
    private final DockerClient dockerClient = NodeWatcher.getDockerClientFactory().forceNewDockerClient();

    private AtomicBoolean doRun = new AtomicBoolean(true);
    private final Set<String> runningContainers = new HashSet<>();


    public ContainerHealthReader() {

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
    }

    @Override
    public void run() {
        while (this.doRun.get()) {
            try {
                synchronized (this.runningContainers) {
                    logger.info("Updating healths for {} containers.", this.runningContainers.size());
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

            } catch (Exception e2) {
                logger.error("Caught exception: {}.", e2.getMessage());
                e2.printStackTrace(System.err);
                CommonsAPI.getInstance().createExceptionNotification(sessionBuilder, e2, NodeWatcher.getMetaData());

            } finally {
                try {
                    this.interpretableSleep(FETCH_INTERVAL);

                } catch (InterruptedException e1) {
                    this.doRun.set(false);
                    logger.warn("Stopping ContainerHealthReader.");
                }
            }
        }
    }


    public void init() {
        synchronized (this.runningContainers) {
            this.runningContainers.clear();
            NWUtils.getLocallyRunningContainersFromDB(sessionBuilder)
                    .forEach(this::onNewContainer);
        }

        logger.info("Initialized with {} containers.", runningContainers.size());
    }


    public void interrupt() {
        this.doRun.set(false);
    }


    /**
     * A custom sleep function, which checks every second if the program should still run and exits if not
     *
     * @param duration
     */
    private void interpretableSleep(long duration) throws InterruptedException {
        for (long i = 0; i < duration; i++) {
            TimeUnit.SECONDS.sleep(1);
        }
    }

    private void fetchContainerHealth(String containerID) {
        InspectContainerResponse response = dockerClient.inspectContainerCmd(containerID).exec();
        HealthState healthState = response.getState().getHealth();

        try (Session session = sessionBuilder.openSession()) {
            DBContainer container = session.get(DBContainer.class, containerID);

            for (HealthStateLog log : healthState.getLog()) {
                Timestamp timestamp = NWUtils.parseDockerTimestamp(log.getStart());

                // Only add health logs, which are newer than the latest logs
                if (container.getContainerHealths() != null && container.getContainerHealths().size() > 0) {
                    Timestamp oldTimestamp = container.getContainerHealths().get(0).getTimestamp();
                    if (oldTimestamp.equals(timestamp) || oldTimestamp.after(timestamp)) {
                        continue;
                    }
                }

                Transaction tx = session.beginTransaction();
                try {
                    DBContainerHealth health = new DBContainerHealth(
                            timestamp,
                            container,
                            this.cleanLogMessage(log.getOutput()),
                            (short) ((long) log.getExitCodeLong())
                    );

                    session.persist(health);
                    tx.commit();

                } catch (Exception e) {
                    tx.rollback();
                    throw e;
                }
            }
        }
    }

    /**
     * Used to clean common error log messages.
     * @param message Message to stream.
     * @return
     */
    private String cleanLogMessage(String message) {
        Matcher m = RE_CLEAN_CURL.matcher(message);

        // Remove irrelevant curl text
        if (m.matches()) {
            message = m.group(2);
        }

        return message;
    }

}