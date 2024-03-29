package de.derteufelqwe.nodewatcher;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.model.*;
import de.derteufelqwe.commons.Constants;
import de.derteufelqwe.commons.Utils;
import de.derteufelqwe.commons.exceptions.DockerMCException;
import de.derteufelqwe.commons.health.HealthCheck;
import de.derteufelqwe.commons.hibernate.SessionBuilder;
import de.derteufelqwe.commons.misc.ServiceMetaData;
import de.derteufelqwe.nodewatcher.exceptions.InvalidSystemStateException;
import de.derteufelqwe.nodewatcher.executors.ContainerEventHandler;
import de.derteufelqwe.nodewatcher.executors.NodeEventHandler;
import de.derteufelqwe.nodewatcher.executors.ServiceEventHandler;
import de.derteufelqwe.nodewatcher.executors.TimedPermissionWatcher;
import de.derteufelqwe.nodewatcher.health.ContainerHealthReader;
import de.derteufelqwe.nodewatcher.health.ServiceHealthReader;
import de.derteufelqwe.nodewatcher.misc.DockerClientFactory;
import de.derteufelqwe.nodewatcher.stats.ContainerResourceWatcher;
import de.derteufelqwe.nodewatcher.stats.HostResourceWatcher;
import lombok.Getter;
import lombok.SneakyThrows;
import lombok.extern.log4j.Log4j2;

import javax.annotation.CheckForNull;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Docker nodes should have a name label
 */
@Log4j2
public class NodeWatcher {

    // ToDo: solve the docker client / thread problems

    private final Pattern RE_MEM_TOTAL = Pattern.compile("MemTotal:\\s+(\\d+).+");

    @Getter private static DockerClientFactory dockerClientFactory;
    @Getter private static SessionBuilder sessionBuilder;
    @Getter private static String swarmNodeId;
    @Getter private static boolean dockerMaster;
    @Getter private static boolean nodeWatcherMaster;
    @Getter private static ServiceMetaData metaData = new ServiceMetaData();

    private DockerClient dockerClient;
    private HealthCheck healthCheck = new HealthCheck();

    // --- Executors ---
    private NodeEventHandler nodeEventHandler;
    private HostResourceWatcher hostResourceWatcherThread;
    private ContainerEventHandler containerEventHandler;
    private ContainerResourceWatcher containerResourceWatcher;
    private TimedPermissionWatcher timedPermissionWatcher;
    private ContainerHealthReader containerHealthReader;
    private ServiceEventHandler serviceEventHandler;
    private ServiceHealthReader serviceHealthReader;


    public NodeWatcher(String dockerHost, SessionBuilder sessionBuilder) {
        NodeWatcher.dockerClientFactory = new DockerClientFactory(dockerHost);
        NodeWatcher.sessionBuilder = sessionBuilder;
        this.dockerClient = dockerClientFactory.getDockerClient();

        swarmNodeId = this.getLocalSwarmNodeId();   // Must be the first value set
        dockerMaster = this.checkForDockerMaster();
        nodeWatcherMaster = this.checkForNodeWatcherMasterLabel();
    }


    /**
     * Starts the NodeEventHandler and waits until it finished its setup
     */
    private void startNodeEventHandler() {
        if (!NodeWatcher.isDockerMaster()) {
            log.warn("This node is no swarm master. Skipping NodeEventHandler.");
            return;

        } else if (!NodeWatcher.isNodeWatcherMaster()) {
            log.warn("This node is no NodeWatcher master. Skipping NodeEventHandler.");
            return;
        }

        this.nodeEventHandler = new NodeEventHandler();
        dockerClient.eventsCmd()
                .withEventTypeFilter(EventType.NODE)
                .withEventFilter("create", "update", "remove")
                .exec(this.nodeEventHandler);

        try {
            if (!this.nodeEventHandler.awaitStarted(60, TimeUnit.SECONDS)) {
                log.error("NodeEventHandler failed to start within 60 seconds.");
            }

        } catch (InterruptedException e) {
            log.error("NodeEventHandler awaiting start interrupted.");
        }
    }

    /**
     * Starts the watcher for service starts / stops
     */
    private void startServiceEventHandler() {
        if (!NodeWatcher.isDockerMaster()) {
            log.warn("This node is no swarm master. Skipping ServiceEventHandler.");
            return;

        } else if (!NodeWatcher.isNodeWatcherMaster()) {
            log.warn("This node is no NodeWatcher master. Skipping ServiceEventHandler.");
            return;
        }

        this.serviceEventHandler = new ServiceEventHandler();

        dockerClient.eventsCmd()
                .withEventTypeFilter(EventType.SERVICE)
                .exec(this.serviceEventHandler);

        try {
            if (!this.serviceEventHandler.awaitStarted(60, TimeUnit.SECONDS)) {
                log.error("ServiceEventHandler failed to start within 60 seconds.");
            }

        } catch (InterruptedException e) {
            log.error("ServiceEventHandler awaiting start interrupted.");
        }
    }

    /**
     * Starts the watcher for container starts / deaths
     */
    private void startContainerWatcher() {
        this.containerEventHandler = new ContainerEventHandler();
        containerEventHandler.addObserver(this.containerResourceWatcher);
        containerEventHandler.addObserver(this.containerHealthReader);

        dockerClient.eventsCmd()
                .withLabelFilter(Constants.DOCKER_IDENTIFIER_MAP)
                .withEventTypeFilter(EventType.CONTAINER)
                .withEventFilter("create", "start", "die")
                .exec(this.containerEventHandler);
    }

    /**
     * Starts the watcher for the docker hosts resources
     */
    private void startHostResourceMonitor() {
        this.hostResourceWatcherThread = new HostResourceWatcher();
        this.hostResourceWatcherThread.start();
    }

    /**
     * Starts the container stats monitor
     */
    private void startContainerResourceWatcher() {
        this.containerResourceWatcher = new ContainerResourceWatcher();
    }

    /**
     * Starts the timed permissions watcher
     */
    private void startTimedPermissionWatcher() {
        this.timedPermissionWatcher = new TimedPermissionWatcher();
        this.timedPermissionWatcher.start();
    }

    /**
     * Starts the container health monitor
     */
    private void startContainerHealthReader() {
        this.containerHealthReader = new ContainerHealthReader();
        this.containerHealthReader.start();
    }

    /**
     * Starts the service health reader if the node is a master node and doesn't have the label 'FETCH_SERVICE_HEALTH=false'
     */
    private void startServiceHealthReader() {
        if (!NodeWatcher.isDockerMaster()) {
            log.warn("This node is no swarm master. Skipping ServiceHealthReader.");
            return;

        } else if (!NodeWatcher.isNodeWatcherMaster()) {
            log.warn("This node is no NodeWatcher master. Skipping ServiceHealthReader.");
            return;
        }

        this.serviceHealthReader = new ServiceHealthReader();
        this.serviceHealthReader.start();
    }


    @SneakyThrows
    public void start() {
        dockerClient.pingCmd().exec();

        this.startNodeEventHandler();
        this.startServiceEventHandler();
        this.startHostResourceMonitor();
        this.startServiceHealthReader();
        this.startContainerResourceWatcher();
        this.startContainerHealthReader();
        this.startContainerWatcher();   // Start last, since most watchers need its events
        this.startTimedPermissionWatcher();

        this.healthCheck.start();
        log.info("NodeWatcher started successfully.");
    }


    @SneakyThrows
    public void stop() {
        if (hostResourceWatcherThread != null) {
            hostResourceWatcherThread.interrupt();
        }
        if (containerEventHandler != null) {
            containerEventHandler.close();
        }
        if (this.timedPermissionWatcher != null) {
            this.timedPermissionWatcher.interrupt();
        }
        if (this.containerHealthReader != null) {
            this.serviceHealthReader.interrupt();
        }
        if (this.serviceHealthReader != null) {
            this.serviceHealthReader.interrupt();
        }

        dockerClient.close();
        sessionBuilder.close();
        this.healthCheck.stop();
    }

    /**
     * Makes non-master node containers wait for the master node container to create the required entries
     */
    private void awaitNodeWatcherMasterStarted() {

    }

    // -----  Utility methods  -----

    /**
     * Analyzes the current nodes labels to determine if this node is allowed to read master data
     * like service events, service health or node events
     * @return
     */
    private boolean checkForNodeWatcherMasterLabel() {
        if (!NodeWatcher.isDockerMaster()) {
            return false;
        }

        List<SwarmNode> nodes = dockerClient.listSwarmNodesCmd()
                .withIdFilter(Collections.singletonList(swarmNodeId))
                .exec();
        if (nodes.size() != 1) {
            throw new DockerMCException("Docker swarm node %s not found. Found %s nodes.", swarmNodeId, nodes.size());
        }
        SwarmNode node = nodes.get(0);

        Map<String, String> labels = node.getSpec().getLabels();
        if (labels.getOrDefault(Constants.NODEWATCHER_MASTER, "true").equals("false")) {
            log.warn("Local node {} has label {}=false. This permits certain features of this NodeWatcher instance.",
                    swarmNodeId, Constants.NODEWATCHER_MASTER);
            return false;
        }

        return true;
    }

    /**
     * Checks if the current node is a docker master node
     * @return
     */
    private boolean checkForDockerMaster() {
        SwarmInfo swarmInfo = dockerClient.infoCmd().exec().getSwarm();
        return swarmInfo.getControlAvailable();
    }

    /**
     * Tries to get the name of a swarm node from its labels
     *
     * @param swarmNode
     * @return
     */
    @CheckForNull
    private String getSwarmName(SwarmNode swarmNode) {
        String name = null;

        SwarmNodeSpec swarmNodeSpec = swarmNode.getSpec();
        if (swarmNodeSpec != null) {
            Map<String, String> labels = swarmNodeSpec.getLabels();

            if (labels != null) {
                name = labels.get("name");
            }
        }

        return name;
    }

    /**
     * Returns the hosts max amount of available RAM
     *
     * @return
     */
    private Integer getMaxHostMemory() throws InvalidSystemStateException {
        String output = Utils.executeCommandOnHost("cat", "/proc/meminfo");
        Matcher m = RE_MEM_TOTAL.matcher(output);

        if (m.find()) {
            try {
                return Integer.parseInt(m.group(1));

            } catch (NumberFormatException e) {
                throw new InvalidSystemStateException("Read invalid meminfo " + m.group(1) + " for local node.");
            }
        }

        throw new InvalidSystemStateException("Failed to find any information about available host RAM.");
    }

    /**
     * Returns the id of the docker swarm node, where this application runs on
     *
     * @return
     */
    private String getLocalSwarmNodeId() throws InvalidSystemStateException {
        Info info = dockerClient.infoCmd().exec();
        SwarmInfo swarmInfo = info.getSwarm();

        if (swarmInfo == null)
            throw new InvalidSystemStateException("SwarmInfo not found. Make sure this node is part of a working docker swarm.");

        return swarmInfo.getNodeID();
    }

}
