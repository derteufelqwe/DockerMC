package de.derteufelqwe.nodewatcher.stats;


import com.github.dockerjava.api.DockerClient;
import de.derteufelqwe.commons.hibernate.SessionBuilder;
import de.derteufelqwe.nodewatcher.misc.INewContainerObserver;
import de.derteufelqwe.nodewatcher.NodeWatcher;

import de.derteufelqwe.nodewatcher.misc.NWUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Set;

/**
 * Reports the stats for docker containers
 */
public class ContainerResourceWatcher implements INewContainerObserver {

    private Logger logger = LogManager.getLogger(getClass().getName());
    private final DockerClient dockerClient = NodeWatcher.getDockerClientFactory().forceNewDockerClient();
    private final SessionBuilder sessionBuilder = NodeWatcher.getSessionBuilder();


    public ContainerResourceWatcher() {

    }


    /**
     * Gets notified when a new container starts
     * @param containerId
     */
    @Override
    public void onNewContainer(String containerId) {
        this.startContainerStat(containerId);
    }

    /**
     * Starts a
     * @param containerId
     */
    private void startContainerStat(String containerId) {
        dockerClient.statsCmd(containerId).exec(new ContainerStatsCallback(containerId));
    }


    /**
     * Start the stats collection for all running containers
     */
    public void init() {
        Set<String> runningContainers = NWUtils.getLocallyRunningContainersFromDB(sessionBuilder);
        for (String id : runningContainers) {
            this.startContainerStat(id);
        }

        logger.info("Initialized with {} containers.", runningContainers.size());
    }


}
