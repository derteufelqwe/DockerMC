package de.derteufelqwe.bungeeplugin;

import com.github.dockerjava.api.model.Event;
import com.github.dockerjava.api.model.EventType;
import com.github.dockerjava.core.command.EventsResultCallback;
import de.derteufelqwe.commons.Constants;
import net.md_5.bungee.api.ProxyServer;

import java.io.IOException;
import java.util.Map;

public class DockerEventHandler implements Runnable {

    private final String CONTAINER_START = "start";
    private final String CONTAINER_STOP = "die";

    private Docker docker;

    public DockerEventHandler() {
        this.docker = BungeePlugin.getDocker();
    }


    @Override
    public void run() {
        System.out.println("Starting Event callback...");

        EventsResultCallback callback = new EventsResultCallback() {
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
                            ContainerProcessor processor = new ContainerProcessor(item);
                            processor.start();
                        }
                    }
                }
            }
        };

        try {
            this.docker.getDocker().eventsCmd().exec(callback)
                    .awaitCompletion()
                    .onError(new RuntimeException("Eventhandler died."));

        } catch (InterruptedException e2) {
            System.out.println("[Warning] Stopping DockerEventHandler!");
        }
    }
}
