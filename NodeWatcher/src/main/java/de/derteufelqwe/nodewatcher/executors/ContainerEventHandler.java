package de.derteufelqwe.nodewatcher.executors;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.async.ResultCallback;
import com.github.dockerjava.api.command.InspectContainerResponse;
import com.github.dockerjava.api.exception.NotFoundException;
import com.github.dockerjava.api.model.Container;
import com.github.dockerjava.api.model.Event;
import com.github.dockerjava.api.model.EventActor;
import de.derteufelqwe.commons.CommonsAPI;
import de.derteufelqwe.commons.Constants;
import de.derteufelqwe.commons.hibernate.LocalSessionRunnable;
import de.derteufelqwe.commons.hibernate.SessionBuilder;
import de.derteufelqwe.commons.hibernate.objects.DBContainer;
import de.derteufelqwe.commons.hibernate.objects.DBService;
import de.derteufelqwe.commons.hibernate.objects.Node;
import de.derteufelqwe.nodewatcher.NodeWatcher;
import de.derteufelqwe.nodewatcher.exceptions.InvalidSystemStateException;

import de.derteufelqwe.nodewatcher.misc.*;
import lombok.extern.log4j.Log4j2;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Range;

import javax.annotation.Nullable;
import java.io.Closeable;
import java.io.IOException;
import java.sql.Timestamp;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Watches for docker container events to update container in the database.
 */
@Log4j2
public class ContainerEventHandler implements ResultCallback<Event> {

    private DockerClient dockerClient = NodeWatcher.getDockerClientFactory().forceNewDockerClient();
    private SessionBuilder sessionBuilder = NodeWatcher.getSessionBuilder();

    private final List<IContainerObserver> observers = new ArrayList<>();


    public ContainerEventHandler() {
    }


    /**
     * Initializes the ContainerWatcher and makes sure that all running containers are stored in the database
     * and containers the stopped while this ContainerWatcher was offline, are updated accordingly
     */
    @Override
    public void onStart(Closeable closeable) {
        try {
            List<Container> containers = NWUtils.getRelevantMCBCContainers(dockerClient);
            this.gatherStartedContainers(containers);
            this.completeStoppedContainers(containers);

        } catch (Exception e) {
            log.error("Initialization failed.", e);
            CommonsAPI.getInstance().createExceptionNotification(sessionBuilder, e, NodeWatcher.getMetaData());
            throw e;
        }
    }

    @Override
    public void onNext(Event object) {
        try {
            switch (object.getStatus()) {
                case "create":
                    this.onContainerCreate(object);
                    break;

                case "start":
                    this.onContainerStart(object);
                    break;

                case "die":
                    this.onContainerDie(object);
                    break;

                default:
                    log.error("Got invalid event type " + object);
            }

        } catch (Exception e) {
            log.error("Exception occurred in the ContainerWatcher.", e);
            CommonsAPI.getInstance().createExceptionNotification(sessionBuilder, e, NodeWatcher.getMetaData());
        }
    }

    @Override
    public void onError(Throwable throwable) {
        log.error("Uncaught exception occurred in the ContainerWatcher.");
        log.error(throwable.getMessage());
    }

    @Override
    public void onComplete() {

    }

    @Override
    public void close() throws IOException {

    }

    // -----  Custom methods  -----

    /**
     * Adds all newly started containers, while the NodeWatcher was offline, to the database
     * @param relevantContainers Minecraft / BungeeCord containers running on this node
     */
    private void gatherStartedContainers(List<Container> relevantContainers) {
        final Set<String> toCreateContainerIDs = new HashSet<>();

        new LocalSessionRunnable(sessionBuilder) {
            @Override
            protected void exec(Session session) {
                for (Container container : relevantContainers) {
                    DBContainer dbContainer = session.get(DBContainer.class, container.getId());
                    if (dbContainer == null) {
                        toCreateContainerIDs.add(container.getId());
                    }
                }
            }
        }.run();

        // Create the DB entries for the unknown containers
        for (String id : toCreateContainerIDs) {
            this.createDBContainer(id);
        }
    }

    /**
     * Finds all stopped containers, that stopped while the NodeWatcher was offline, and marks them as stopped in the database
     * @param relevantContainers Minecraft / BungeeCord containers running on this node
     */
    private void completeStoppedContainers(List<Container> relevantContainers) {
        Set<String> existingDBContainerIDs = NWUtils.getLocallyRunningContainersFromDB(sessionBuilder);
        List<String> runningContainerIDs = relevantContainers.stream()
                .map(Container::getId)
                .collect(Collectors.toList());

        for (String containerID : existingDBContainerIDs) {
            // Only process not running containers
            if (runningContainerIDs.contains(containerID)) {
                continue;
            }

            new LocalSessionRunnable(sessionBuilder) {
                @Override
                protected void exec(Session session) {
                    DBContainer dbContainer = session.get(DBContainer.class, containerID);
                    if (dbContainer == null) {
                        log.error("Failed to find container object for {}.", containerID);
                        return;
                    }

                    try {
                        InspectContainerResponse response = dockerClient.inspectContainerCmd(containerID).exec();
                        completeDBContainer(
                                containerID,
                                NWUtils.parseDockerTimestamp(response.getState().getFinishedAt()),
                                response.getState().getExitCodeLong().shortValue()
                        );

                    // Container was deleted while the NodeWatcher was offline
                    } catch (NotFoundException e) {
                        completeDBContainer(containerID, new Timestamp(System.currentTimeMillis()), (short) 51);
                    }
                }
            }.run();
        }

    }

    public void addOberserv(IContainerObserver observer) {
        this.observers.add(observer);
    }

    // -----  Event handler methods  -----

    /**
     * Saves a container to the database when its started
     *
     * @param event
     */
    private void onContainerCreate(Event event) {
        this.createDBContainer(event.getId());
    }

    private void onContainerStart(Event event) {
        this.extendDBContainer(event.getId());
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

        this.completeDBContainer(event.getId(), new Timestamp(event.getTime() * 1000), exitCode);
    }

    /**
     * Creates a database entry for a docker container
     *
     * @param id Container id
     */
    private void createDBContainer(String id) {
        InspectContainerResponse cont = dockerClient.inspectContainerCmd(id).exec();
        String nodeId = this.getNodeId(cont);
        String serviceId = this.getContainerServiceId(cont);
        String taskId = this.getContainerTaskId(cont);
        short taskSlot = this.getTaskSlot(cont);


        try (Session session = this.sessionBuilder.openSession()) {
            Transaction tx = session.beginTransaction();

            try {
                DBService dbService = session.get(DBService.class, serviceId);
                if (dbService == null) {
                    throw new InvalidSystemStateException("Service %s not found.", serviceId);
                }

                Node node = session.get(Node.class, nodeId);
                if (node == null) {
                    throw new InvalidSystemStateException("Node %s not found.", nodeId);
                }

                DBContainer container = new DBContainer(
                        id,
                        cont.getConfig().getImage(),
                        cont.getName().substring(1),
                        taskId,
                        taskSlot,
                        node,
                        dbService
                );

                // SaveOrUpdate is required when restarting a stopped container. Otherwise a unique constraint violation would be thrown
                session.persist(container);

            } finally {
                tx.commit();
            }

            log.info("Created container entry {}.", id);
        }

        // Notify observers
        for (IContainerObserver observer : this.observers) {
            observer.onNewContainer(id);
        }
    }

    /**
     * Adds information to an existing db container, which is only available when it started
     * @param id
     */
    private void extendDBContainer(String id) {
        InspectContainerResponse container = dockerClient.inspectContainerCmd(id).exec();

        new LocalSessionRunnable(sessionBuilder) {
            @Override
            protected void exec(Session session) {
                DBContainer dbContainer = session.get(DBContainer.class, container.getId());
                if (dbContainer == null) {
                    throw new InvalidSystemStateException("Container %s not found.", container.getId());
                }

                dbContainer.setStartTime(NWUtils.parseDockerTimestamp(container.getState().getStartedAt()));
                dbContainer.setIp(container.getNetworkSettings().getNetworks().get(Constants.NETW_OVERNET_NAME).getIpAddress());

                session.update(dbContainer);
            }
        }.run();

        log.info("Modified container entry {}.", id);
    }

    /**
     * "Completes" a container in the database. This means that its stop timestamp and exit code are added
     *
     * @param id
     */
    private void completeDBContainer(String id, Timestamp stopTime, Short exitCode) {
        try (Session session = this.sessionBuilder.openSession()) {
            Transaction tx = session.beginTransaction();

            try {
                DBContainer container = session.get(DBContainer.class, id);
                if (container == null) {
                    log.error("Stopped Container with id {} not found!", id);
                    return;
                }

                container.setStopTime(stopTime);
                container.setExitcode(exitCode);

                session.update(container);
                log.info("Finished container entry {}.", id);

            } finally {
                tx.commit();
            }

        }

        // Notify observers
        for (IContainerObserver observer : this.observers) {
            observer.onRemoveContainer(id);
        }
    }


    // -----  Utility methods  -----

    /**
     * Tries to get the containers exit code from its attributes
     *
     * @return
     */
    @NotNull
    private Short getContainerExitCode(@Nullable Map<String, String> attributes) {
        if (attributes == null) {
            return -1;
        }

        try {
            return Short.parseShort(attributes.get("exitCode"));

        } catch (NumberFormatException ignored) {
            return -2;
        }
    }

    /**
     * Tries to read a label from a container and throws an exception if it's not found
     * @param container
     * @return
     */
    @NotNull
    private String getContainerLabel(InspectContainerResponse container, String label) throws InvalidSystemStateException {
        Map<String, String> labels = container.getConfig().getLabels();

        if (labels != null) {
            String labelData = labels.get(label);

            if (labelData != null && !labelData.equals("")) {
                return labelData;
            }
        }

        throw new InvalidSystemStateException("Container %s has no label '%s'.", container.getId(), label);
    }

    /**
     * Tries to get the containers swarm node id from its labels
     *
     * @param container
     * @return
     */
    @NotNull
    private String getNodeId(InspectContainerResponse container) throws InvalidSystemStateException {
        return getContainerLabel(container, "com.docker.swarm.node.id");
    }

    /**
     * Tries to get the task slot from the container object.
     * @return Return value > 0 = Task slot, < 0 = Error code.
     */
    @Range(from = 0, to = Short.MAX_VALUE)
    private short getTaskSlot(InspectContainerResponse container) throws InvalidSystemStateException {
        String taskName = getContainerLabel(container, "com.docker.swarm.task.name");
        String[] nameSplits = taskName.split("\\.");
        if (nameSplits.length != 3) {
            throw new InvalidSystemStateException("Containers %s taskname '%s' is invalid.", container.getId(), taskName);
        }

        String numberStr = nameSplits[1];
        try {
            return Short.parseShort(numberStr);

        } catch (NumberFormatException e) {
            throw new InvalidSystemStateException("Containers %s taskname '%s' is invalid. %s is not short.", container.getId(), taskName, numberStr);
        }
    }

    /**
     * Tries to get the containers service id from its labels
     *
     * @param container
     * @return
     */
    @NotNull
    private String getContainerServiceId(InspectContainerResponse container) throws InvalidSystemStateException {
        return getContainerLabel(container, "com.docker.swarm.service.id");
    }

    /**
     * Tries to get the containers services task id from its labels
     *
     * @param container
     * @return
     */
    @NotNull
    private String getContainerTaskId(InspectContainerResponse container) throws InvalidSystemStateException {
        return getContainerLabel(container, "com.docker.swarm.task.id");
    }

}
