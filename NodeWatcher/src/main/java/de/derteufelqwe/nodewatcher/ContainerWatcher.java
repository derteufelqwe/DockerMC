package de.derteufelqwe.nodewatcher;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.async.ResultCallback;
import com.github.dockerjava.api.command.InspectContainerResponse;
import com.github.dockerjava.api.model.Event;
import com.github.dockerjava.api.model.EventActor;
import de.derteufelqwe.commons.hibernate.SessionBuilder;
import de.derteufelqwe.commons.hibernate.objects.Container;
import de.derteufelqwe.commons.hibernate.objects.Node;
import de.derteufelqwe.nodewatcher.misc.INewContainerObserver;
import de.derteufelqwe.nodewatcher.misc.InvalidSystemStateException;
import org.hibernate.Session;
import org.hibernate.Transaction;

import javax.annotation.CheckForNull;
import java.io.Closeable;
import java.io.IOException;
import java.sql.Timestamp;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;

/**
 * Watches for docker events.
 */
public class ContainerWatcher implements ResultCallback<Event> {

    private DockerClient dockerClient = NodeWatcher.getDockerClient();
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
     */
    public void init() {
        this.addContainerEntry("5aefcf23eae67f80c777486888a87d37cd02340e0775db2b24d4409729188160");
    }

    public void addNewContainerObserver(INewContainerObserver newContainerObserver) {
        if (newContainerObserver != null) {
            this.newContainerObservers.add(newContainerObserver);
        }
    }

    @CheckForNull
    private String getNodeId(InspectContainerResponse containerResponse) {
        Map<String, String> labels = containerResponse.getConfig().getLabels();
        if (labels != null) {
            return labels.get("com.docker.swarm.node.id");
        }

        return null;
    }

    /**
     * Saves a container to the database when its started
     *
     * @param event
     */
    private void onContainerStart(Event event) {
        this.addContainerEntry(event.getId());
    }


    @CheckForNull
    private Timestamp parseTimestamp(String tsRaw) {
        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd'T'hh:mm:ss.SSS");
        format.setTimeZone(TimeZone.getTimeZone("UTC"));

        try {
            return new Timestamp(format.parse(tsRaw).getTime());

        } catch (ParseException e) {
            return null;
        }
    }


    private void addContainerEntry(String id) {
        InspectContainerResponse cont = dockerClient.inspectContainerCmd(id).exec();
        Container container = new Container(id, cont.getConfig().getImage(), this.parseTimestamp(cont.getCreated()));  // getFrom, //getTime
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

                // Notify observers
                for (INewContainerObserver observer : this.newContainerObservers) {
                    observer.onNewContainer(id);
                }

            } finally {
                tx.commit();
            }

        }
    }

    @CheckForNull
    private Short getContainerExitCode(Event event) {
        EventActor actor = event.getActor();
        if (actor == null) {
            return null;
        }

        Map<String, String> labels = actor.getAttributes();
        if (labels == null) {
            return null;
        }

        try {
            return Short.parseShort(labels.get("exitCode"));

        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    private void onContainerDie(Event event) {

        try (Session session = this.sessionBuilder.openSession()) {
            Transaction tx = session.beginTransaction();

            try {
                Container container = session.get(Container.class, event.getId());
                if (container == null) {
                    System.err.println("Stopped Container with id " + event.getId() + " not found!");
                    return;
                }

                container.setStopTime(new Timestamp(event.getTime() * 1000));
                container.setExitcode(this.getContainerExitCode(event));

                session.update(container);
                System.out.println("[ContainerWatcher] Updated container entry " + event.getId() + ".");

            } finally {
                tx.commit();
            }

        }
    }

}
