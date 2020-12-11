package de.derteufelqwe.nodewatcher;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.async.ResultCallback;
import com.github.dockerjava.api.model.Event;
import de.derteufelqwe.commons.Constants;
import de.derteufelqwe.commons.hibernate.SessionBuilder;
import de.derteufelqwe.commons.hibernate.objects.Container;
import org.hibernate.Session;
import org.hibernate.Transaction;

import java.io.Closeable;
import java.io.IOException;
import java.sql.Timestamp;
import java.util.Map;

/**
 * Callback class for the docker event listener, for parse stopped dockermc containers.
 */
public class LogsEventCallback implements ResultCallback<Event> {

    private DockerClient dockerClient;
    private SessionBuilder sessionBuilder;


    public LogsEventCallback(DockerClient dockerClient, SessionBuilder sessionBuilder) {
        this.dockerClient = dockerClient;
        this.sessionBuilder = sessionBuilder;
    }


    private Container getContainer(Event event, String log, Timestamp lastLogTimestamp) {
        Map<String, String> labels = null;
        if (event.getActor() != null) {
            labels = event.getActor().getAttributes();
        }

//        Container container = new Container(event.getId(), event.getFrom(), log,
//                new Timestamp(event.getTime() * 1000), lastLogTimestamp);
//
//        if (labels != null) {
//            container.setContainerName(labels.get("name"));
//            container.setServerName(labels.get("ServerName"));
//            container.setNodeId(labels.get("com.docker.swarm.node.id"));
//            try {
//                container.setExitCode(Short.parseShort(labels.get("exitCode")));
//            } catch (NumberFormatException ignored) {
//            }
//        }

//        return container;
        return null;
    }


    @Override

    public void onStart(Closeable closeable) {

    }

    @Override
    public void onNext(Event object) {
        Map<String, String> labels = null;
        if (object.getActor() != null) {
            labels = object.getActor().getAttributes();
        }

        // Ignore all containers without the required labels
        if (labels == null) {
            return;
        } else {
            String type = labels.get(Constants.CONTAINER_IDENTIFIER_KEY);
            if (type == null || !(type.equals(Constants.ContainerType.MINECRAFT.name()) ||
                    type.equals(Constants.ContainerType.BUNGEE.name()))) {
                return;
            }
        }

        // Container handling
        Session session = this.sessionBuilder.openSession();
        Transaction tx = session.beginTransaction();

        try {
            String id = object.getId();

            System.out.println("Container " + id + " stopped.");

            LogDownloadCallback logDownloadCallback = this.dockerClient.logContainerCmd(id)
                    .withStdOut(true)
                    .withStdErr(true)
                    .withTimestamps(true)
                    .exec(new LogDownloadCallback());

            logDownloadCallback.await();

            Container container = this.getContainer(object, logDownloadCallback.getLogMessage(), logDownloadCallback.getLastTimestamp());
            session.persist(container);

            session.flush();
            tx.commit();

        } catch (RuntimeException e) {
            tx.rollback();
            System.err.println("Error while saving logs for " + object.getId() + ":");
            e.printStackTrace();

        } finally {
            session.close();
        }
    }

    @Override
    public void onError(Throwable throwable) {
        System.err.println(throwable);
    }

    @Override
    public void onComplete() {

    }

    @Override
    public void close() throws IOException {
        this.sessionBuilder.close();
    }
}
