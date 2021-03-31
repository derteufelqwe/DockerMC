package de.derteufelqwe.nodewatcher.health;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.model.Service;
import com.github.dockerjava.api.model.Task;
import de.derteufelqwe.commons.CommonsAPI;
import de.derteufelqwe.commons.Constants;
import de.derteufelqwe.commons.hibernate.SessionBuilder;
import de.derteufelqwe.nodewatcher.NodeWatcher;
import de.derteufelqwe.nodewatcher.executors.ContainerWatcher;
import de.derteufelqwe.nodewatcher.misc.RepeatingThread;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.jetbrains.annotations.NotNull;

import javax.persistence.NoResultException;
import java.sql.Timestamp;
import java.util.List;
import java.util.stream.Collectors;


/**
 * Responsible for periodically downloading the new logs for a container.
 * Containers are added by {@link ContainerWatcher}
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
            List<String> serviceIDs = this.getRelevantServiceIDs();
            logger.info("Updating healths for {} services.", serviceIDs.size());

            for (String serviceID : serviceIDs) {
                this.fetchServiceHealth(serviceID);
            }

        } catch (Exception e) {
            logger.error("ServiceHealthReader caught exception.", e);
            CommonsAPI.getInstance().createExceptionNotification(sessionBuilder, e, NodeWatcher.getMetaData());
        }
    }


    private List<String> getRelevantServiceIDs() {
        List<String> services = dockerClient.listServicesCmd()
                .exec().stream()
                .filter(s -> s.getSpec() != null)
                .filter(s -> s.getSpec().getLabels() != null)
                .filter(s ->
                        s.getSpec().getLabels().get(Constants.CONTAINER_IDENTIFIER_KEY).equals(Constants.ContainerType.MINECRAFT_POOL.name()) ||
                                s.getSpec().getLabels().get(Constants.CONTAINER_IDENTIFIER_KEY).equals(Constants.ContainerType.BUNGEE_POOL.name())
                )
                .map(Service::getId)
                .collect(Collectors.toList());

        return services;
    }

    // ToDo: Create all nodes in the db if the node is a manager

    private void fetchServiceHealth(String serviceID) {
        Service service = dockerClient.inspectServiceCmd(serviceID).exec();
        List<Task> tasks = dockerClient.listTasksCmd()
                .withServiceFilter(service.getId())
                .exec();

        this.getLatestServiceHealthLog(serviceID);

        System.out.println("");
    }


    /**
     * Returns the timestamp of the latest service health log from the container
     * @return
     */
    @NotNull
    private Timestamp getLatestServiceHealthLog(String serviceID) {
        try (Session session = sessionBuilder.openSession()) {

            try {
                Object res = session.createNativeQuery(
                        "SELECT sh.timestamp FROM service_healths AS sh WHERE sh.service_id = :sid ORDER BY sh.timestamp desc LIMIT 1"
                ).setParameter("sid", serviceID).getSingleResult();

                return (Timestamp) res;

            } catch (NoResultException e) {
                return new Timestamp(0);
            }

        }
    }


}
