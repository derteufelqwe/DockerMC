package de.derteufelqwe.bungeeplugin;

import com.github.dockerjava.api.command.InspectContainerResponse;
import com.github.dockerjava.api.model.Event;
import de.derteufelqwe.commons.Constants;
import net.md_5.bungee.api.ProxyServer;

import java.net.InetSocketAddress;
import java.util.Map;

public class ContainerProcessor extends Thread {

    private final String SERVICE_KEY = "com.docker.swarm.service.id";
    private final String SERVICE_NAME = "com.docker.swarm.service.name";
    private final String TASK_NAME = "com.docker.swarm.task.name";
    private final String CONTAINER_START = "start";
    private final String CONTAINER_STOP = "die";

    private Docker docker;
    private ProxyServer proxy;
    private Event item;

    public ContainerProcessor(Event item) {
        this.docker = BungeePlugin.getDocker();
        this.proxy = ProxyServer.getInstance();
        this.item = item;
    }

    @Override
    public void run() {
        Map<String, String> attribs = this.item.getActor().getAttributes();

        if (attribs.get(SERVICE_KEY) != null) {
            processServiceContainer(item);

        } else {
            processSingleContainer(item);
        }
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

}
