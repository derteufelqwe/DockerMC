package de.derteufelqwe.nodewatcher.stats;


import com.github.dockerjava.api.DockerClient;
import de.derteufelqwe.commons.hibernate.SessionBuilder;
import de.derteufelqwe.nodewatcher.NodeWatcher;
import de.derteufelqwe.nodewatcher.misc.IContainerObserver;
import de.derteufelqwe.nodewatcher.misc.NWUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Set;

/**
 * Reports the stats for docker containers
 */
public class ContainerResourceWatcher implements IContainerObserver {

    private Logger logger = LogManager.getLogger(getClass().getName());
    private final DockerClient dockerClient = NodeWatcher.getDockerClientFactory().forceNewDockerClient();
    private final SessionBuilder sessionBuilder = NodeWatcher.getSessionBuilder();


    public ContainerResourceWatcher() {

    }


    /**
     * Gets notified when a new container starts
     *
     * @param containerId
     */
    @Override
    public void onNewContainer(String containerId) {
        this.startContainerStat(containerId);
    }

    @Override
    public void onRemoveContainer(String containerId) {

    }

    /**
     * Starts a
     *
     * @param containerId
     */
    private void startContainerStat(String containerId) {
        dockerClient.statsCmd(containerId).exec(new ContainerStatsCallback(containerId));
    }



}
