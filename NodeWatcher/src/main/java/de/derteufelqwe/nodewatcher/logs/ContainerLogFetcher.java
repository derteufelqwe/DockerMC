package de.derteufelqwe.nodewatcher.logs;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.exception.NotFoundException;
import de.derteufelqwe.commons.CommonsAPI;
import de.derteufelqwe.commons.hibernate.SessionBuilder;
import de.derteufelqwe.commons.hibernate.objects.DBContainer;
import de.derteufelqwe.nodewatcher.executors.ContainerWatcher;
import de.derteufelqwe.nodewatcher.misc.*;
import de.derteufelqwe.nodewatcher.NodeWatcher;
import lombok.SneakyThrows;
import org.apache.logging.log4j.Logger;
import org.checkerframework.checker.units.qual.A;
import org.hibernate.Session;
import org.hibernate.Transaction;

import java.sql.Timestamp;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;


/**
 * Responsible for periodically downloading the new logs for a container.
 * Containers are added by {@link ContainerWatcher} and removed by {@link LogLoadCallback}
 */
public class ContainerLogFetcher extends Thread implements INewContainerObserver, IRemoveContainerObserver {

    private final int FETCH_INTERVAL = 10;  // in seconds
    private final int MAX_FAILS = 2;    // Number of fails until the log download gets stopped for a container

    private Logger logger = NodeWatcher.getLogger();
    private final SessionBuilder sessionBuilder = NodeWatcher.getSessionBuilder();
    private final DockerClient dockerClient = NodeWatcher.getDockerClientFactory().forceNewDockerClient();

    private AtomicBoolean doRun = new AtomicBoolean(true);
    private final Set<String> runningContainers = new HashSet<>();
    private Map<String, Short> failureCounts = new HashMap<>();


    public ContainerLogFetcher() {

    }


    @Override
    public void onNewContainer(String containerId) {
        synchronized (this.runningContainers) {
            this.failureCounts.put(containerId, (short) 0);
            this.runningContainers.add(containerId);
        }
    }

    @Override
    public void onRemoveContainer(String containerId) {
        synchronized (this.runningContainers) {
            this.runningContainers.remove(containerId);
            this.failureCounts.remove(containerId);
        }
    }

    @SneakyThrows
    @Override
    public void run() {
        while (this.doRun.get()) {
            try {
                synchronized (this.runningContainers) {
                    for (String id : new HashSet<>(this.runningContainers)) {
                        short failureCount = this.failureCounts.get(id);

                        try {
                            this.updateContainerLogs(id);

                            // Container not found in DB
                        } catch (DBContainerNotFoundException e1) {
                            this.failureCounts.put(id, (short) (failureCount + 1));
                            logger.error(LogPrefix.LOGS + e1.getMessage() + " ({}/{})", failureCount + 1, MAX_FAILS);

                            // Container not found on host
                        } catch (NotFoundException e2) {
                            this.failureCounts.put(id, (short) (failureCount + 1));
                            logger.error(LogPrefix.LOGS + "Container {} not found on host." + " ({}/{})", id, failureCount + 1, MAX_FAILS);
                        }

                        // Check if the container should be removed from log download
                        if (failureCount + 1 >= MAX_FAILS) {
                            this.onRemoveContainer(id);
                            logger.warn(LogPrefix.LOGS + "Removed container {} from log download as it doesn't exist anymore.", id);
                        }
                    }
                }

                this.interpretableSleep(FETCH_INTERVAL);

            } catch (InterruptedException e1) {
                this.doRun.set(false);
                logger.warn(LogPrefix.LOGS + "Stopping ContainerLogFetcher.");

            } catch (Exception e2) {
                logger.error(LogPrefix.LOGS + "Caught exception: {}.", e2.getMessage());
                e2.printStackTrace(System.err);
                CommonsAPI.getInstance().createExceptionNotification(sessionBuilder, e2, NodeWatcher.getMetaData());
            }
        }
    }


    public void init() {
        synchronized (this.runningContainers) {
            this.runningContainers.clear();
            NWUtils.getLocallyRunningContainersFromDB(sessionBuilder)
                    .forEach(this::onNewContainer);
        }

        logger.info(LogPrefix.LOGS + "Initialized with {} containers.", runningContainers.size());
    }


    public void interrupt() {
        this.doRun.set(false);
    }


    /**
     * A custom sleep function, which checks every second if the program should still run and exits if not
     * @param duration
     */
    private void interpretableSleep(long duration) throws InterruptedException {
        for (long i = 0; i < duration; i++) {
            TimeUnit.SECONDS.sleep(1);
        }
    }

    /**
     * Gets the log download timestamp from the database if possible
     *
     * @param containerId
     * @return
     */
    private int getLastLogTimestampForDocker(String containerId) throws DBContainerNotFoundException {
        try (Session session = sessionBuilder.openSession()) {
            Transaction tx = session.beginTransaction();

            try {
                DBContainer container = session.get(DBContainer.class, containerId);
                if (container == null) {
                    throw new DBContainerNotFoundException(containerId);
                }

                Timestamp timestamp = container.getLastLogTimestamp();
                if (timestamp == null)
                    return 0;

                return (int) (timestamp.getTime() / 1000);

            } finally {
                tx.commit();
            }
        }
    }

    /**
     * Starts the container log download. Its {@link LogLoadCallback} will update the log in the database.
     * @param containerId
     */
    private void updateContainerLogs(String containerId) throws DBContainerNotFoundException {
        dockerClient.logContainerCmd(containerId)
                .withStdOut(true)
                .withStdErr(true)
                .withTimestamps(true)
                .withSince(this.getLastLogTimestampForDocker(containerId))
                .exec(new LogLoadCallback(containerId, this));
    }


}
