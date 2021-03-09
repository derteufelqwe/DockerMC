package de.derteufelqwe.nodewatcher.executors;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.async.ResultCallback;
import com.github.dockerjava.api.command.InspectContainerResponse;
import com.github.dockerjava.api.exception.NotFoundException;
import com.github.dockerjava.api.model.Container;
import com.github.dockerjava.api.model.Event;
import com.github.dockerjava.api.model.EventActor;
import com.github.dockerjava.api.model.Service;
import de.derteufelqwe.commons.Constants;
import de.derteufelqwe.commons.Utils;
import de.derteufelqwe.commons.hibernate.SessionBuilder;
import de.derteufelqwe.commons.hibernate.objects.DBContainer;
import de.derteufelqwe.commons.hibernate.objects.DBService;
import de.derteufelqwe.commons.hibernate.objects.Node;
import de.derteufelqwe.nodewatcher.NodeWatcher;
import de.derteufelqwe.nodewatcher.misc.INewContainerObserver;
import de.derteufelqwe.nodewatcher.misc.InvalidSystemStateException;
import de.derteufelqwe.nodewatcher.misc.LogPrefix;
import de.derteufelqwe.nodewatcher.misc.NWUtils;
import org.apache.logging.log4j.Logger;
import org.hibernate.Session;
import org.hibernate.Transaction;

import javax.annotation.CheckForNull;
import javax.annotation.Nullable;
import java.io.Closeable;
import java.io.IOException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Watches for docker events.
 */
public class ContainerWatcher implements ResultCallback<Event> {

    private Logger logger = NodeWatcher.getLogger();
    private DockerClient dockerClient = NodeWatcher.getDockerClientFactory().forceNewDockerClient();
    private SessionBuilder sessionBuilder = NodeWatcher.getSessionBuilder();

    private final List<INewContainerObserver> newContainerObservers = new ArrayList<>();


    public ContainerWatcher() {
    }


    /**
     * Initializes the ContainerWatcher and makes sure that all running containers are stored in the database
     * and containers the stopped while this ContainerWatcher was offline, are updated accordingly
     */
    @Override
    public void onStart(Closeable closeable) {
        List<Container> containers = this.getRunningBungeeMinecraftContainers();
        this.gatherStartedContainers(containers);
        this.completeStoppedContainers(containers);
    }

    @Override
    public void onNext(Event object) {
        try {
            switch (object.getStatus()) {
                case "start":
                    this.onContainerStart(object);
                    break;

                case "die":
                    this.onContainerDie(object);
                    break;

                default:
                    logger.error(LogPrefix.CW + "Got invalid event type " + object);
            }

        } catch (Exception e) {
            logger.error(LogPrefix.CW + "Exception occurred in the ContainerWatcher.");
            logger.error(LogPrefix.CW + e.getMessage());
            e.printStackTrace(System.err);
        }
    }

    @Override
    public void onError(Throwable throwable) {
        logger.error(LogPrefix.CW + "Uncaught exception occurred in the ContainerWatcher.");
        logger.error(LogPrefix.CW + throwable.getMessage());
    }

    @Override
    public void onComplete() {

    }

    @Override
    public void close() throws IOException {

    }

    // -----  Custom methods  -----

    /**
     * Finds all stopped containers, that stopped while the NodeWatcher was offline, and marks them as stopped in the database
     * @param relevantContainers Minecraft / BungeeCord containers running on this node
     */
    private void completeStoppedContainers(List<Container> relevantContainers) {
        Set<String> existingDbContainerIds = NWUtils.getLocallyRunningContainersFromDB(sessionBuilder);
        List<String> containerIds = relevantContainers.stream().map(Container::getId).collect(Collectors.toList());

        try (Session session = sessionBuilder.openSession()) {
            for (String containerId : existingDbContainerIds) {
                // Only execute for not running containers
                if (containerIds.contains(containerId)) {
                    continue;
                }

                Transaction tx = session.beginTransaction();
                try {
                    DBContainer dbContainer = session.get(DBContainer.class, containerId);
                    if (dbContainer == null) {
                        logger.error(LogPrefix.CW + "Failed to find container object for {}.", containerId);
                        continue;
                    }

                    try {
                        InspectContainerResponse response = dockerClient.inspectContainerCmd(containerId).exec();
                        this.finishContainerEntry(containerId, NWUtils.parseDockerTimestamp(response.getState().getFinishedAt()), response.getState().getExitCodeLong().shortValue());

                    // Container was deleted while the NodeWatcher was offline
                    } catch (NotFoundException e) {
                        this.finishContainerEntry(containerId, new Timestamp(System.currentTimeMillis()), (short) 51);
                    }

                } finally {
                    tx.commit();
                }
            }
        }


    }

    /**
     * Adds all newly started containers, while the NodeWatcher was offline, to the database
     * @param relevantContainers Minecraft / BungeeCord containers running on this node
     */
    private void gatherStartedContainers(List<Container> relevantContainers) {
        try (Session session = sessionBuilder.openSession()) {
            for (Container container : relevantContainers) {
                Transaction tx = session.beginTransaction();

                try {
                    DBContainer dbContainer = session.get(DBContainer.class, container.getId());
                    if (dbContainer != null) {
                        continue;
                    }

                    this.addContainerEntry(container.getId());

                } finally {
                    tx.commit();
                }
            }
        }

    }

    public void addNewContainerObserver(INewContainerObserver newContainerObserver) {
        if (newContainerObserver != null) {
            this.newContainerObservers.add(newContainerObserver);
        }
    }


    /**
     * Saves a container to the database when its started
     *
     * @param event
     */
    private void onContainerStart(Event event) {
        this.addContainerEntry(event.getId());
    }

    /**
     * Creates a database entry for a docker container
     *
     * @param id Container id
     */
    private void addContainerEntry(String id) {
        InspectContainerResponse cont = dockerClient.inspectContainerCmd(id).exec();
        String nodeId = this.getNodeId(cont);
        String serviceId = this.getContainerServiceId(cont);
        String taskId = this.getContainerTaskId(cont);
        String ipString = cont.getNetworkSettings().getNetworks().get(Constants.NETW_OVERNET_NAME).getIpAddress();
        short taskSlot = this.getTaskSlot(cont);

        if (serviceId == null) {
            throw new InvalidSystemStateException(LogPrefix.CW + "Container %s has no information about its service.", id);
        }
        if (taskId == null) {
            throw new InvalidSystemStateException(LogPrefix.CW + "Container %s has no information about its task id.", id);
        }
        if (taskSlot < 0) {
            throw new InvalidSystemStateException(LogPrefix.CW + "Container %s has not information about its task slot.", id);
        }

        DBService dbService = this.getOrCreateService(serviceId);

        try (Session session = this.sessionBuilder.openSession()) {
            Transaction tx = session.beginTransaction();

            try {
                if (nodeId == null) {
                    throw new InvalidSystemStateException(LogPrefix.CW + "OnContainerStart for %s failed to find node %s.", id, nodeId);
                }

                Node node = session.get(Node.class, nodeId);
                if (node == null) {
                    throw new InvalidSystemStateException(LogPrefix.CW + "Node %s not found.", nodeId);
                }

                DBContainer container = new DBContainer(
                        id,
                        cont.getConfig().getImage(),
                        NWUtils.parseDockerTimestamp(cont.getCreated()),
                        cont.getName().substring(1),
                        taskId,
                        ipString,
                        taskSlot,
                        node,
                        dbService
                );

                // SaveOrUpdate is required when restarting a stopped container. Otherwise a unique constraint violation would be thrown
                session.saveOrUpdate(container);
                logger.info(LogPrefix.CW + "Created container entry " + id + ".");

            } finally {
                tx.commit();
            }
        }

        // Notify observers
        for (INewContainerObserver observer : this.newContainerObservers) {
            observer.onNewContainer(id);
        }
    }

    /**
     * Called, when a container dies
     *
     * @param event
     */
    private void onContainerDie(Event event) {
        EventActor actor = event.getActor();
        Short exitCode = null;
        if (actor != null) {
            exitCode = this.getContainerExitCode(event.getActor().getAttributes());
        }

        this.finishContainerEntry(event.getId(), new Timestamp(event.getTime() * 1000), exitCode);
    }

    /**
     * "Finishes" a container in the database. This means that its stop timestamp and exit code are saved
     *
     * @param id
     */
    private void finishContainerEntry(String id, Timestamp stopTime, Short exitCode) {
        try (Session session = this.sessionBuilder.openSession()) {
            Transaction tx = session.beginTransaction();

            try {
                DBContainer container = session.get(DBContainer.class, id);
                if (container == null) {
                    logger.error(LogPrefix.CW + "Stopped Container with id {} not found!", id);
                    return;
                }

                container.setStopTime(stopTime);
                container.setExitcode(exitCode);

                session.update(container);
                logger.info(LogPrefix.CW + "Finished container entry {}.", id);

            } finally {
                tx.commit();
            }

        }
    }

    // -----  Utility methods  -----

    /**
     * Tries to get the containers exit code from its attributes
     *
     * @return
     */
    @CheckForNull
    private Short getContainerExitCode(@Nullable Map<String, String> labels) {
        if (labels == null) {
            return null;
        }

        try {
            return Short.parseShort(labels.get("exitCode"));

        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    /**
     * Tries to get the containers swarm node id from its labels
     *
     * @param containerResponse
     * @return
     */
    @CheckForNull
    private String getNodeId(InspectContainerResponse containerResponse) {
        Map<String, String> labels = containerResponse.getConfig().getLabels();
        if (labels != null) {
            return labels.get("com.docker.swarm.node.id");
        }

        return null;
    }

    /**
     * Tries to get the task slot from the container object.
     * @return Return value > 0 = Task slot, < 0 = Error code.
     */
    private short getTaskSlot(InspectContainerResponse containerResponse) {
        String[] nameSplits = containerResponse.getName().split("\\.");
        if (nameSplits.length != 3) {
            return -1;
        }

        try {
            return Short.parseShort(nameSplits[1]);

        } catch (NumberFormatException e) {
            return -2;
        }
    }

    /**
     * Returns all docker containers, which are currently running Minecraft or BungeeCord
     *
     * @return
     */
    private List<Container> getRunningBungeeMinecraftContainers() {
        List<Container> bungeeContainers =    dockerClient.listContainersCmd().withLabelFilter(Utils.quickLabel(Constants.ContainerType.BUNGEE)).exec();
        List<Container> minecraftContainers = dockerClient.listContainersCmd().withLabelFilter(Utils.quickLabel(Constants.ContainerType.MINECRAFT)).exec();

        bungeeContainers.addAll(minecraftContainers);

        return bungeeContainers;
    }

    /**
     * Tries to get the containers service id from its labels
     *
     * @param containerResponse
     * @return
     */
    @CheckForNull
    private String getContainerServiceId(InspectContainerResponse containerResponse) {
        Map<String, String> labels = containerResponse.getConfig().getLabels();
        if (labels != null) {
            return labels.get("com.docker.swarm.service.id");
        }

        return null;
    }

    /**
     * Gets or creates the service entry in the database
     *
     * @param serviceId
     * @return
     */
    private DBService getOrCreateService(String serviceId) {
        try (Session session = sessionBuilder.openSession()) {
            Transaction tx = session.beginTransaction();

            try {
                DBService dbService = session.get(DBService.class, serviceId);
                if (dbService != null) {
                    return dbService;
                }

                Service service = dockerClient.inspectServiceCmd(serviceId).exec();

                dbService = new DBService(
                        service.getId(),
                        service.getSpec().getName(),
                        new Long(service.getSpec().getTaskTemplate().getResources().getLimits().getMemoryBytes() / 1024 / 1024).intValue(),
                        (float) (service.getSpec().getTaskTemplate().getResources().getLimits().getNanoCPUs() / 1000000000.0),
                        service.getSpec().getLabels().get("Type")
                );

                session.persist(dbService);

                logger.info(LogPrefix.CW + "Added new Service {}.", dbService.getId());
                return dbService;

            } finally {
                tx.commit();
            }
        }
    }

    /**
     * Tries to get the containers services task id from its labels
     *
     * @param containerResponse
     * @return
     */
    @CheckForNull
    private String getContainerTaskId(InspectContainerResponse containerResponse) {
        Map<String, String> labels = containerResponse.getConfig().getLabels();
        if (labels != null) {
            return labels.get("com.docker.swarm.task.id");
        }

        return null;
    }

}
