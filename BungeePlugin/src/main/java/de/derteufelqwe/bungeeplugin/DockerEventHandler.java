package de.derteufelqwe.bungeeplugin;

import com.github.dockerjava.api.async.ResultCallback;
import com.github.dockerjava.api.model.Event;
import com.github.dockerjava.api.model.EventType;
import com.github.dockerjava.core.command.EventsResultCallback;
import de.derteufelqwe.commons.Constants;
import net.md_5.bungee.api.ProxyServer;

import java.io.Closeable;
import java.io.IOException;
import java.util.Map;

public class DockerEventHandler extends Thread {

    private final String CONTAINER_START = "start";
    private final String CONTAINER_STOP = "die";
    private ResultCallback<Event> callback;

    private Docker docker;

    public DockerEventHandler(Docker docker) {
        this.docker = docker;
    }


    @Override
    public void run() {
        System.out.println("Starting Event callback...");
        this.callback = new ResultCallback<Event>() {

            @Override
            public void onStart(Closeable closeable) {

            }

            @Override
            public void onError(Throwable throwable) {
                System.err.println("Error " + throwable);
            }

            @Override
            public void onComplete() {

            }

            @Override
            public void close() throws IOException {
                System.out.println("Closing Eventhandler");
            }

            @Override
            public void onNext(Event item) {
                if (item.getType() == EventType.CONTAINER) {
                    Map<String, String> attribs = item.getActor().getAttributes();

                    if (item.getStatus().equals(CONTAINER_START) || item.getStatus().equals(CONTAINER_STOP)) {
                        // Has the tags Owner and Type
                        if (Constants.DOCKER_IDENTIFIER_VALUE.equals(attribs.get(Constants.DOCKER_IDENTIFIER_KEY)) &&
                                Constants.ContainerType.MINECRAFT.name().equals(attribs.get(Constants.CONTAINER_IDENTIFIER_KEY))) {

                            // Add the servers in a new Thread to prevent
                            // 'io.netty.util.concurrent.BlockingOperationException: DefaultChannelPromise(incomplete)'
                            ContainerProcessor processor = new ContainerProcessor(item, docker);
                            processor.start();
                        }
                    }
                }
            }
        };

        this.docker.getDocker().eventsCmd().exec(callback);
    }

    @Override
    public void interrupt() {
        try {
            this.callback.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

        super.interrupt();
    }


}
