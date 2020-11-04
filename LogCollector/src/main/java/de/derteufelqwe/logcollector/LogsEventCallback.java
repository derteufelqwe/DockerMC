package de.derteufelqwe.logcollector;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.async.ResultCallback;
import com.github.dockerjava.api.model.Event;
import de.derteufelqwe.commons.Constants;
import org.hibernate.Session;
import org.hibernate.Transaction;

import java.io.Closeable;
import java.io.IOException;
import java.util.Map;

public class LogsEventCallback implements ResultCallback<Event> {

    private DockerClient dockerClient;
    private SessionBuilder sessionBuilder;


    public LogsEventCallback(DockerClient dockerClient) {
        this.dockerClient = dockerClient;
        this.sessionBuilder = new SessionBuilder("admin", "password", "postgresdb:5432", false);
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

            LogCallback logCallback = this.dockerClient.logContainerCmd(id)
                    .withStdOut(true)
                    .withStdErr(true)
                    .withTimestamps(true)
                    .exec(new LogCallback());

            logCallback.await();

            Container container = new Container(object, logCallback.getLogMessage(), logCallback.getLastTimestamp());
            session.persist(container);

            session.flush();
            tx.commit();

        } catch (RuntimeException e) {
            tx.rollback();
            throw e;

        } finally {
            session.close();
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
        this.sessionBuilder.close();
    }
}
