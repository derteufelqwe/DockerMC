package de.derteufelqwe.nodewatcher;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.async.ResultCallback;
import com.github.dockerjava.api.command.InspectContainerResponse;
import com.github.dockerjava.api.model.Container;
import com.github.dockerjava.api.model.Event;
import com.github.dockerjava.api.model.EventActor;
import com.github.dockerjava.api.model.ServicePlacement;
import de.derteufelqwe.commons.Constants;
import de.derteufelqwe.commons.Utils;
import de.derteufelqwe.commons.hibernate.SessionBuilder;
import de.derteufelqwe.commons.hibernate.objects.DBContainer;
import de.derteufelqwe.commons.hibernate.objects.Node;
import de.derteufelqwe.nodewatcher.misc.INewContainerObserver;
import de.derteufelqwe.nodewatcher.misc.InvalidSystemStateException;
import de.derteufelqwe.nodewatcher.misc.NWUtils;
import org.hibernate.Session;
import org.hibernate.Transaction;

import javax.annotation.CheckForNull;
import javax.annotation.Nullable;
import java.io.Closeable;
import java.io.IOException;
import java.sql.Timestamp;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Watches for docker events.
 */
public class ContainerWatcher implements ResultCallback<Event> {

    private DockerClient dockerClient = NodeWatcher.getDockerClientFactory().forceNewDockerClient();
    private SessionBuilder sessionBuilder = NodeWatcher.getSessionBuilder();

    private final List<INewContainerObserver> newContainerObservers = new ArrayList<>();


    public ContainerWatcher() {
    }


    @Override
    public void onStart(Closeable closeable) {

    }

    @Override
    public void onNext(Event object) {
        switch (object.getStatus()) {
            case "start":
                this.onContainerStart(object);
                break;

            case "die":
                this.onContainerDie(object);
                break;

            default:
                System.err.println("Got invalid event type " + object);
        }
    }

    @Override
    public void onError(Throwable throwable) {

    }

    @Override
    public void onComplete() {

    }

    @Override
    public void close() throws IOException {

    }

    // -----  Custom methods  -----

    /**
     * Initializes the ContainerWatcher and makes sure that all running containers are stored in the database
     * and containers the stopped while this ContainerWatcher was offline, are updated accordingly
     */
    public void init() {
        List<Container> containers = this.getRunningBungeeMinecraftContainers();
        List<String> containerIds = containers.stream().map(Container::getId).collect(Collectors.toList());

        // Create new container entries if they are not yet present in the database.
        try (Session session = sessionBuilder.openSession()) {
            for (Container container : containers) {
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

        // Finish the database entries for stopped containers
        Set<String> existingDbContainerIds = NWUtils.findLocalRunningContainers(sessionBuilder);

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
                        System.err.println("[ContainerWatcher] Failed to find container object for " + containerId + ".");
                        continue;
                    }

                    InspectContainerResponse response = dockerClient.inspectContainerCmd(containerId).exec();
                    this.finishContainerEntry(containerId, NWUtils.parseDockerTimestamp(response.getState().getFinishedAt()), response.getState().getExitCodeLong().shortValue());

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
     * @param id Container id
     */
    private void addContainerEntry(String id) {
        InspectContainerResponse cont = dockerClient.inspectContainerCmd(id).exec();
        DBContainer container = new DBContainer(id, cont.getConfig().getImage(), NWUtils.parseDockerTimestamp(cont.getCreated()));
        container.setName(cont.getName());
        container.setMaxRam((int) (cont.getHostConfig().getMemory() / 1024 / 1024));

        String nodeId = this.getNodeId(cont);

        try (Session session = this.sessionBuilder.openSession()) {
            Transaction tx = session.beginTransaction();

            try {
                if (nodeId == null) {
                    throw new InvalidSystemStateException("OnContainerStart for %s failed to find node %s.", id, nodeId);
                }

                Node node = session.get(Node.class, nodeId);
                container.setNode(node);

                session.persist(container);
                System.out.println("[ContainerWatcher] Created container entry " + id + ".");

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
     * @param id
     */
    private void finishContainerEntry(String id, Timestamp stopTime, Short exitCode) {
        try (Session session = this.sessionBuilder.openSession()) {
            Transaction tx = session.beginTransaction();

            try {
                DBContainer container = session.get(DBContainer.class, id);
                if (container == null) {
                    System.err.println("Stopped Container with id " + id + " not found!");
                    return;
                }

                container.setStopTime(stopTime);
                container.setExitcode(exitCode);

                session.update(container);
                System.out.println("[ContainerWatcher] Updated container entry " + id + ".");

            } finally {
                tx.commit();
            }

        }
    }

    // -----  Utility methods  -----

    /**
     * Tries to get the containers exit code from its attributes
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
     * Returns all docker containers, which are currently cunning
     * @return
     */
    private List<Container> getRunningBungeeMinecraftContainers() {
        List<Container> bungeeContainers = dockerClient.listContainersCmd().withLabelFilter(Utils.quickLabel(Constants.ContainerType.BUNGEE)).exec();

        List<Container> minecraftContainers = dockerClient.listContainersCmd().withLabelFilter(Utils.quickLabel(Constants.ContainerType.MINECRAFT)).exec();

        bungeeContainers.addAll(minecraftContainers);

        return bungeeContainers;
    }

}
