package de.derteufelqwe.logcollector;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.async.ResultCallback;
import com.github.dockerjava.api.model.Event;

import java.io.Closeable;
import java.io.IOException;

public class LogsEventCallback implements ResultCallback<Event> {

    private DockerClient dockerClient;

    public LogsEventCallback(DockerClient dockerClient) {
        this.dockerClient = dockerClient;
    }

    @Override
    public void onStart(Closeable closeable) {

    }

    @Override
    public void onNext(Event object) {
        String id = object.getId();

        System.out.println("Container " + id + " died.");

        LogCallback logCallback = this.dockerClient.logContainerCmd(id)
                .withStdOut(true)
                .withStdErr(true)
                .withTimestamps(true)
                .exec(new LogCallback());

        logCallback.await();

//        System.out.println(logCallback.getLogMessage());
        System.out.println(logCallback.getLastTimestamp());
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
}
