package de.derteufelqwe.bungeeplugin;

import com.github.dockerjava.api.command.InspectContainerResponse;
import com.github.dockerjava.api.exception.NotFoundException;
import com.github.dockerjava.api.model.Container;
import com.github.dockerjava.api.model.Event;
import com.github.dockerjava.api.model.EventType;
import com.github.dockerjava.core.command.EventsResultCallback;
import de.derteufelqwe.commons.Constants;
import net.md_5.bungee.api.ProxyServer;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.Map;

public class DockerEventHandler implements Runnable {

    private final String SERVICE_KEY = "com.docker.swarm.service.id";
    private final String SERVICE_NAME = "com.docker.swarm.service.name";
    private final String TASK_NAME = "com.docker.swarm.task.name";
    private final String CONTAINER_START = "start";
    private final String CONTAINER_STOP = "die";

    private Docker docker;
    private ProxyServer proxy;

    public DockerEventHandler() {
        this.docker = BungeePlugin.getDocker();
        proxy = ProxyServer.getInstance();
    }

    private void startContainer(Event item) {
        System.out.println("Starting container " + item.getId());
        InspectContainerResponse container = docker.getDocker().inspectContainerCmd(item.getId())
                .exec();

        String ip = container.getNetworkSettings().getNetworks().get("overnet").getIpAddress();
        String name = container.getName().substring(1);
        proxy.getConfig().addServer(
                proxy.constructServerInfo(name, new InetSocketAddress(ip, 25565),
                        "Lobbyserver " + container.getName(), false));

        System.out.println("Added server " + container.getName());
    }

    private void stopContainer(Event item) {
        System.out.println("Stopping container " + item.getId());
        InspectContainerResponse container = docker.getDocker().inspectContainerCmd(item.getId())
                .exec();

        String name = container.getName().substring(1);
        proxy.getConfig().removeServerNamed(name);

        System.out.println("Removed server " + container.getName());
    }


    private void addServer(String serverName, Event item) {
        InspectContainerResponse container = docker.getDocker().inspectContainerCmd(item.getId())
                .exec();
        String ip = container.getNetworkSettings().getNetworks().get(Constants.NETW_OVERNET_NAME).getIpAddress();

        proxy.getConfig().addServer(
                proxy.constructServerInfo(serverName, new InetSocketAddress(ip, 25565),
                        String.format("Server %s running in %s.", serverName, container.getId()) , false));

        System.out.println("Added server " + serverName + ".");
    }


    private void removeServer(String serverName) {
        proxy.getConfig().removeServerNamed(serverName);
        System.out.println("Removed server " + serverName + ".");
    }


    private void processSingleContainer(Event item) {
        Map<String, String> attribs = item.getActor().getAttributes();
        String serverName = attribs.get(Constants.SERVER_NAME_KEY);

        if (serverName == null) {
            System.err.println(String.format("Container %s has no name key %s.", item.getId(), Constants.SERVER_NAME_KEY));
            return;
        }

        if (item.getStatus().equals(CONTAINER_START)) {
            this.addServer(serverName, item);

        } else if (item.getStatus().equals(CONTAINER_STOP)) {
            this.removeServer(serverName);
        }

    }


    private void processServiceContainer(Event item) {
        Map<String, String> attribs = item.getActor().getAttributes();
        String serviceName = attribs.get(SERVICE_NAME);
        String taskName = attribs.get(TASK_NAME);
        String[] splitTask = taskName.substring(serviceName.length()).split("\\.");
        String serverName = attribs.get(Constants.SERVER_NAME_KEY);

        if (splitTask.length < 2) {
            System.err.println(String.format("Container %s has weired task name %s.", item.getId(), taskName));
            return;
        }
        if (serverName == null) {
            System.err.println(String.format("Container %s has no name key %s.", item.getId(), Constants.SERVER_NAME_KEY));
            return;
        }
        serverName += "-" + splitTask[1];

        if (item.getStatus().equals(CONTAINER_START)) {
            this.addServer(serverName, item);

        } else if (item.getStatus().equals(CONTAINER_STOP)) {
            this.removeServer(serverName);
        }
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

                            if (attribs.get(SERVICE_KEY) != null) {
                                processServiceContainer(item);

                            } else {
                                processSingleContainer(item);
                            }

                        }
                    }

                    // ------------------
//                    if ("DockerMC".equals(attribs.get("Owner")) && "MINECRAFT".equals(attribs.get("Type"))) {
//                        if (item.getStatus().equals("start")) {
//                            try {
//                                startContainer(item);
//
//                            } catch (Exception e) {
//                                System.out.println(String.format("[Info] Couldn't start container. %s", e.getMessage()));
//                            }
//
//                        } else if (item.getStatus().equals("die")) {
//                            try {
//                                stopContainer(item);
//
//                            } catch (Exception e) {
//                                System.out.println(String.format("[Info] Couldn't remove container. %s", e.getMessage()));
//                            }
//                        }
//                    }

                }
            }
        };

        try {
            BungeePlugin.getDocker().getDocker().eventsCmd().exec(callback).awaitCompletion().close();

        } catch (IOException e) {
            e.printStackTrace();

        } catch (InterruptedException e2) {
            System.out.println("[Warning] Stopping DockerEventHandler!");
        }
    }
}
