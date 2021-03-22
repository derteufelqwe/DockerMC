package de.derteufelqwe.nodewatcher.health;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.HealthState;
import com.github.dockerjava.api.command.HealthStateLog;
import com.github.dockerjava.api.command.InspectContainerResponse;
import com.github.dockerjava.api.exception.NotFoundException;
import com.github.dockerjava.api.model.Service;
import de.derteufelqwe.commons.CommonsAPI;
import de.derteufelqwe.commons.hibernate.SessionBuilder;
import de.derteufelqwe.commons.hibernate.objects.DBContainer;
import de.derteufelqwe.commons.hibernate.objects.DBContainerHealth;
import de.derteufelqwe.nodewatcher.NodeWatcher;
import de.derteufelqwe.nodewatcher.executors.ContainerWatcher;
import de.derteufelqwe.nodewatcher.logs.LogLoadCallback;
import de.derteufelqwe.nodewatcher.misc.*;
import org.apache.logging.log4j.Logger;
import org.hibernate.Session;
import org.hibernate.Transaction;

import java.sql.Timestamp;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


/**
 * Responsible for periodically downloading the new logs for a container.
 * Containers are added by {@link ContainerWatcher} and removed by {@link LogLoadCallback}
 */
public class ServiceHealthReader extends RepeatingThread {

    private Logger logger = NodeWatcher.getLogger();
    private final SessionBuilder sessionBuilder = NodeWatcher.getSessionBuilder();
    private final DockerClient dockerClient = NodeWatcher.getDockerClientFactory().forceNewDockerClient();


    public ServiceHealthReader() {
        super(20);
    }

    @Override
    public void repeatedRun() {
        try {
            // Find all relevant services

            logger.info(LogPrefix.SHEALTH + "Updating healths for {} services.", 2);
            for (String id : new HashSet<>(this.runningContainers)) {

                try {
                    this.fetchContainerHealth(id);

                    // Container not found in DB
                } catch (DBContainerNotFoundException e1) {
                    logger.error(LogPrefix.SHEALTH + e1.getMessage());
                    this.runningContainers.remove(id);

                    // Container not found on host
                } catch (NotFoundException e2) {
                    logger.error(LogPrefix.SHEALTH + "Container {} not found on host.", id);
                    this.runningContainers.remove(id);
                }

            }

        }  catch (Exception e) {
            logger.error(LogPrefix.SHEALTH + "Caught exception: {}.", e.getMessage());
            e.printStackTrace(System.err);
            CommonsAPI.getInstance().createExceptionNotification(sessionBuilder, e, NodeWatcher.getMetaData());
        }
    }


    private List<Service> get


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

}
