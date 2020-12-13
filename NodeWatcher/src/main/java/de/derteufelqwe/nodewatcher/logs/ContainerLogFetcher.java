package de.derteufelqwe.nodewatcher.logs;

import com.github.dockerjava.api.DockerClient;
import de.derteufelqwe.commons.hibernate.SessionBuilder;
import de.derteufelqwe.commons.hibernate.objects.Container;
import de.derteufelqwe.nodewatcher.ContainerWatcher;
import de.derteufelqwe.nodewatcher.misc.INewContainerObserver;
import de.derteufelqwe.nodewatcher.misc.NWUtils;
import de.derteufelqwe.nodewatcher.NodeWatcher;
import lombok.SneakyThrows;
import org.hibernate.Session;
import org.hibernate.Transaction;

import java.sql.Timestamp;
import java.util.Set;
import java.util.concurrent.TimeUnit;


/**
 * Responsible for periodically downloading the new logs for a container.
 * Containers are added by {@link ContainerWatcher} and removed by {@link LogLoadCallback}
 */
public class ContainerLogFetcher extends Thread implements INewContainerObserver {

    private final int FETCH_INTERVAL = 5;  // in seconds

    private final SessionBuilder sessionBuilder = NodeWatcher.getSessionBuilder();
    private final DockerClient dockerClient = NodeWatcher.getDockerClient();

    private boolean doRun = true;
    private Set<String> runningContainers;


    public ContainerLogFetcher() {

    }


    @Override
    public void onNewContainer(String containerId) {
        synchronized (this.runningContainers) {
            this.runningContainers.add(containerId);
        }
    }

    public synchronized void removeContainer(String containerId) {
        this.runningContainers.remove(containerId);
    }


    @SuppressWarnings("SynchronizeOnNonFinalField") // Just dont change the field runningContainers
    @SneakyThrows
    @Override
    public void run() {
        this.runningContainers = NWUtils.findLocalRunningContainers(sessionBuilder);
        System.out.println("Initialized ContainerLogFetcher with " + runningContainers.size() + " containers.");

        while (this.doRun) {
            synchronized (this.runningContainers) {
                for (String id : this.runningContainers) {
                    this.updateContainerLogs(id);
                }
            }
            System.out.println("updated " + this.runningContainers.size());

            this.interruptableSleep(FETCH_INTERVAL);
        }

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
                Container container = session.get(Container.class, containerId);
                if (container == null) {
                    System.err.println("Failed to get container for " + containerId + "!");
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
