package de.derteufelqwe.nodewatcher.logs;

import com.github.dockerjava.api.DockerClient;
import de.derteufelqwe.commons.hibernate.SessionBuilder;
import de.derteufelqwe.commons.hibernate.objects.DBContainer;
import de.derteufelqwe.nodewatcher.executors.ContainerWatcher;
import de.derteufelqwe.nodewatcher.misc.INewContainerObserver;
import de.derteufelqwe.nodewatcher.misc.IRemoveContainerObserver;
import de.derteufelqwe.nodewatcher.misc.NWUtils;
import de.derteufelqwe.nodewatcher.NodeWatcher;
import lombok.SneakyThrows;
import org.apache.logging.log4j.Logger;
import org.hibernate.Session;
import org.hibernate.Transaction;

import java.sql.Timestamp;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.TimeUnit;


/**
 * Responsible for periodically downloading the new logs for a container.
 * Containers are added by {@link ContainerWatcher} and removed by {@link LogLoadCallback}
 */
public class ContainerLogFetcher extends Thread implements INewContainerObserver, IRemoveContainerObserver {

    private final int FETCH_INTERVAL = 10;  // in seconds

    private Logger logger = NodeWatcher.getLogger();
    private final SessionBuilder sessionBuilder = NodeWatcher.getSessionBuilder();
    private final DockerClient dockerClient = NodeWatcher.getDockerClientFactory().forceNewDockerClient();

    private boolean doRun = true;
    private final Set<String> runningContainers = Collections.synchronizedSet(new HashSet<>());


    public ContainerLogFetcher() {

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

    @SneakyThrows
    @Override
    public void run() {
        while (this.doRun) {
            synchronized (this.runningContainers) {
                for (String id : this.runningContainers) {
                    this.updateContainerLogs(id);
                }
            }

            this.interruptableSleep(FETCH_INTERVAL);
        }
    }


    public void init() {
        this.runningContainers.clear();
        this.runningContainers.addAll(NWUtils.getLocallyRunningContainersFromDB(sessionBuilder));
        logger.info("[ContainerLogFetcher] Initialized with " + runningContainers.size() + " containers.");
    }


    public void interrupt() {
        this.doRun = false;
    }


    /**
     * A custom sleep function, which checks every second if the program should still run and exits if not
     * @param duration
     */
    private void interruptableSleep(long duration) throws InterruptedException {
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
    private int getLastLogTimestampForDocker(String containerId) {
        try (Session session = sessionBuilder.openSession()) {
            Transaction tx = session.beginTransaction();

            try {
                DBContainer container = session.get(DBContainer.class, containerId);
                if (container == null) {
                    logger.error("Failed to get container for {} !", containerId);
                    return 0;
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
    private void updateContainerLogs(String containerId) {
        dockerClient.logContainerCmd(containerId)
                .withStdOut(true)
                .withStdErr(true)
                .withTimestamps(true)
                .withSince(this.getLastLogTimestampForDocker(containerId))
                .exec(new LogLoadCallback(containerId, this));
    }


}
