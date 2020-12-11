package de.derteufelqwe.nodewatcher;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.async.ResultCallback;
import com.github.dockerjava.api.command.InspectContainerResponse;
import com.github.dockerjava.api.model.Event;
import com.github.dockerjava.api.model.EventActor;
import de.derteufelqwe.commons.hibernate.SessionBuilder;
import de.derteufelqwe.commons.hibernate.objects.Container;
import de.derteufelqwe.commons.hibernate.objects.Node;
import org.hibernate.Session;
import org.hibernate.Transaction;

import javax.annotation.CheckForNull;
import javax.persistence.criteria.CriteriaBuilder;
import java.io.Closeable;
import java.io.IOException;
import java.net.Inet4Address;
import java.sql.Timestamp;
import java.util.Map;

/**
 * Watches for docker events.
 */
public class ContainerWatcher implements ResultCallback<Event> {

    private DockerClient dockerClient = NodeWatcher.getDockerClient();
    private SessionBuilder sessionBuilder = NodeWatcher.getSessionBuilder();


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
     * @param event
     */
    private void onContainerStart(Event event) {
        InspectContainerResponse cont = dockerClient.inspectContainerCmd(event.getId()).exec();
        Container container = new Container(event.getId(), event.getFrom(), new Timestamp(event.getTime() * 1000));
        container.setName(cont.getName());
        container.setMaxRam((int) (cont.getHostConfig().getMemory() / 1024 / 1024));

        String nodeId = this.getNodeId(cont);

        try (Session session = this.sessionBuilder.openSession()) {
            Transaction tx = session.beginTransaction();
            try {
                if (nodeId != null) {
                    Node node = session.get(Node.class, nodeId);
                    container.setNode(node);
                }

                session.persist(container);
                System.out.println("Created container entry " + event.getId() + ".");

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
                    System.err.println("Container with id " + event.getId() + " not found!");
                    return;
                }

                container.setStopTime(new Timestamp(event.getTime() * 1000));
                container.setExitcode(this.getContainerExitCode(event));

                session.update(container);
                System.out.println("Updated container entry " + event.getId() + ".");

            } finally {
                tx.commit();
            }

        }
    }

}
