package de.derteufelqwe.bungeeplugin;

import com.github.dockerjava.api.command.InspectContainerResponse;
import com.github.dockerjava.api.model.Container;
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
    private InspectContainerResponse containerResponse;
    private String containerStatus;     // Start or die


    public ContainerProcessor(Docker docker, String containerID) {
        this.docker = docker;
        this.proxy = ProxyServer.getInstance();

        this.containerResponse = this.docker.getDocker().inspectContainerCmd(containerID).exec();
        this.containerStatus = CONTAINER_START;
    }

    public ContainerProcessor(Docker docker, Event event) {
        this(docker, event.getId());
        this.containerStatus = event.getStatus();
    }

    public ContainerProcessor(Docker docker, Container container) {
        this(docker, container.getId());
        this.containerStatus = CONTAINER_START;
    }


    @Override
    public void run() {
        Map<String, String> attribs = this.containerResponse.getConfig().getLabels();

        if (attribs.get(SERVICE_KEY) != null) {
            processServiceContainer();

        } else {
            processSingleContainer();
        }
    }


    public void addServer(String serverName, InspectContainerResponse containerResponse) {
        String ip = containerResponse.getNetworkSettings().getNetworks().get(Constants.NETW_OVERNET_NAME).getIpAddress();

        proxy.getConfig().addServer(
                proxy.constructServerInfo(serverName, new InetSocketAddress(ip, 25565),
                        String.format("Server %s running in %s.", serverName, containerResponse.getId()) , false));

        System.out.println("Added server " + serverName + ".");
    }


    public void removeServer(String serverName) {
        proxy.getConfig().removeServerNamed(serverName);
        System.out.println("Removed server " + serverName + ".");
    }


    private void processSingleContainer() {
        Map<String, String> attribs = this.containerResponse.getConfig().getLabels();
        String serverName = attribs.get(Constants.SERVER_NAME_KEY);

        if (serverName == null) {
            System.err.println(String.format("Container %s has no name key %s.", this.containerResponse.getId(), Constants.SERVER_NAME_KEY));
            return;
        }

        if (this.containerStatus.equals(CONTAINER_START)) {
            this.addServer(serverName, this.containerResponse);

        } else if (this.containerResponse.equals(CONTAINER_STOP)) {
            this.removeServer(serverName);
        }

    }


    /**
     * Generates the name of a container by analyzing the labels of a container.
     * @return
     */
    private String getServerName(InspectContainerResponse containerResponse) {
        Map<String, String> labels = containerResponse.getConfig().getLabels();
        String serviceName = labels.get(SERVICE_NAME);
        String taskName = labels.get(TASK_NAME);
        String[] splitTask = taskName.substring(serviceName.length()).split("\\.");
        String serverName = labels.get(Constants.SERVER_NAME_KEY);

        if (splitTask.length < 2) {
            throw new RuntimeException(String.format("Container %s has weired task name %s.", containerResponse.getId(), taskName));
        }
        if (serverName == null) {
            throw new RuntimeException(String.format("Container %s has no name key %s.", containerResponse.getId(), Constants.SERVER_NAME_KEY));
        }

        serverName += "-" + splitTask[1];

        return serverName;
    }


    private void processServiceContainer() {
        String serverName = this.getServerName(this.containerResponse);

        if (this.containerStatus.equals(CONTAINER_START)) {
            this.addServer(serverName, this.containerResponse);

        } else if (this.containerStatus.equals(CONTAINER_STOP)) {
            this.removeServer(serverName);
        }
    }

}
