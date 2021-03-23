package de.derteufelqwe.nodewatcher.health;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.model.Service;
import de.derteufelqwe.commons.CommonsAPI;
import de.derteufelqwe.commons.Constants;
import de.derteufelqwe.commons.hibernate.SessionBuilder;
import de.derteufelqwe.nodewatcher.NodeWatcher;
import de.derteufelqwe.nodewatcher.executors.ContainerWatcher;

import de.derteufelqwe.nodewatcher.logs.LogLoadCallback;
import de.derteufelqwe.nodewatcher.misc.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.List;
import java.util.stream.Collectors;


/**
 * Responsible for periodically downloading the new logs for a container.
 * Containers are added by {@link ContainerWatcher} and removed by {@link LogLoadCallback}
 */
public class ServiceHealthReader extends RepeatingThread {

    private Logger logger = LogManager.getLogger(getClass().getName());
    private final SessionBuilder sessionBuilder = NodeWatcher.getSessionBuilder();
    private final DockerClient dockerClient = NodeWatcher.getDockerClientFactory().forceNewDockerClient();


    public ServiceHealthReader() {
        super(20);
    }

    @Override
    public void repeatedRun() {
        try {
            // Find all relevant services
            List<Service> services = this.getRelevantServices();
            logger.info("Updating healths for {} services.", services.size());

//            for (String id : services) {
//
//                try {
//                    this.fetchContainerHealth(id);
//
//                    // Container not found in DB
//                } catch (DBContainerNotFoundException e1) {
//                    logger.error(LogPrefix.SHEALTH + e1.getMessage());
//                    this.runningContainers.remove(id);
//
//                    // Container not found on host
//                } catch (NotFoundException e2) {
//                    logger.error(LogPrefix.SHEALTH + "Container {} not found on host.", id);
//                    this.runningContainers.remove(id);
//                }
//
//            }

        }  catch (Exception e) {
            logger.error("Caught exception: {}.", e.getMessage());
            e.printStackTrace(System.err);
            CommonsAPI.getInstance().createExceptionNotification(sessionBuilder, e, NodeWatcher.getMetaData());
        }
    }


    private List<Service> getRelevantServices() {
        List<Service> services = dockerClient.listServicesCmd()
                .exec().stream()
                .filter(s -> s.getSpec() != null)
                .filter(s -> s.getSpec().getLabels() != null)
                .filter(s ->
                        s.getSpec().getLabels().get(Constants.CONTAINER_IDENTIFIER_KEY).equals(Constants.ContainerType.MINECRAFT_POOL.name()) ||
                        s.getSpec().getLabels().get(Constants.CONTAINER_IDENTIFIER_KEY).equals(Constants.ContainerType.BUNGEE_POOL.name())
                )
                .collect(Collectors.toList());

        return services;
    }


    private void fetchContainerHealth(String containerID) {

    }

}
