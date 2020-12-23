package de.derteufelqwe.nodewatcher.stats;


import com.github.dockerjava.api.DockerClient;
import de.derteufelqwe.commons.hibernate.SessionBuilder;
import de.derteufelqwe.nodewatcher.misc.INewContainerObserver;
import de.derteufelqwe.nodewatcher.NodeWatcher;
import de.derteufelqwe.nodewatcher.misc.NWUtils;
import lombok.SneakyThrows;

import java.util.List;
import java.util.Set;

/**
 * Reports the stats for docker containers
 */
public class ContainerResourceWatcher implements INewContainerObserver {

    private final DockerClient dockerClient = NodeWatcher.getDockerClientFactory().forceNewDockerClient();
    private final SessionBuilder sessionBuilder = NodeWatcher.getSessionBuilder();


    public ContainerResourceWatcher() {

    }


    @Override
    public void onNewContainer(String containerId) {
        this.startContainerStat(containerId);
    }

    private void startContainerStat(String containerId) {
        dockerClient.statsCmd(containerId).exec(new ContainerStatsCallback(containerId));
    }


    /**
     * Start the stats collection for all running containers
     */
    public void init() {
        Set<String> runningContainers = NWUtils.findLocalRunningContainers(sessionBuilder);
        for (String id : runningContainers) {
            this.startContainerStat(id);
        }

        System.out.println("[ContainerResourceWatcher] Initialized with " + runningContainers.size() + " containers.");
    }


}
