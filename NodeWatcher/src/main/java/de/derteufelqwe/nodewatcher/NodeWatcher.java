package de.derteufelqwe.nodewatcher;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.model.*;
import de.derteufelqwe.commons.Constants;
import de.derteufelqwe.commons.Utils;
import de.derteufelqwe.commons.exceptions.DockerMCException;
import de.derteufelqwe.commons.hibernate.SessionBuilder;
import de.derteufelqwe.commons.hibernate.objects.Node;
import de.derteufelqwe.commons.misc.ServiceMetaData;
import de.derteufelqwe.nodewatcher.executors.ContainerWatcher;
import de.derteufelqwe.nodewatcher.executors.ServiceWatcher;
import de.derteufelqwe.nodewatcher.executors.TimedPermissionWatcher;
import de.derteufelqwe.nodewatcher.health.ContainerHealthReader;
import de.derteufelqwe.nodewatcher.health.ServiceHealthReader;
import de.derteufelqwe.nodewatcher.logs.ContainerLogFetcher;
import de.derteufelqwe.nodewatcher.misc.DockerClientFactory;
import de.derteufelqwe.nodewatcher.exceptions.InvalidSystemStateException;

import de.derteufelqwe.nodewatcher.stats.ContainerResourceWatcher;
import de.derteufelqwe.nodewatcher.stats.HostResourceWatcher;
import lombok.Getter;
import lombok.SneakyThrows;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.hibernate.Session;
import org.hibernate.Transaction;

import javax.annotation.CheckForNull;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Docker nodes should have a name label
 */
public class NodeWatcher {

    // ToDo: solve the docker client / thread problems

    private final Pattern RE_MEM_TOTAL = Pattern.compile("MemTotal:\\s+(\\d+).+");

    @Getter
    private static Logger logger = LogManager.getLogger(NodeWatcher.class.getName());

    @Getter private static DockerClientFactory dockerClientFactory;
    @Getter private static SessionBuilder sessionBuilder;
    @Getter private static String swarmNodeId;
    @Getter private static ServiceMetaData metaData = new ServiceMetaData();

    private DockerClient dockerClient;

    // --- Executors ---
    private HostResourceWatcher hostResourceWatcherThread;
    private ContainerWatcher containerWatcher;
    private ContainerLogFetcher logFetcher;
    private ContainerResourceWatcher containerResourceWatcher;
    private TimedPermissionWatcher timedPermissionWatcher;
    private ContainerHealthReader containerHealthReader;
    private ServiceWatcher serviceWatcher;
    private ServiceHealthReader serviceHealthReader;


    public NodeWatcher(String dockerHost, SessionBuilder sessionBuilder) {
        NodeWatcher.dockerClientFactory = new DockerClientFactory(dockerHost);
        NodeWatcher.sessionBuilder = sessionBuilder;
        this.dockerClient = dockerClientFactory.getDockerClient();
        swarmNodeId = this.getLocalSwarmNodeId();
    }


    /**
     * Saves the docker swarm nodes in the database
     */
    private void saveSwarmNode() {
        try (Session session = sessionBuilder.openSession()) {
            Transaction tx = session.beginTransaction();

            try {
                Node node = session.get(Node.class, swarmNodeId);

                // Node already known
                if (node != null) {
                    logger.info("Local node already known.");
                    return;
                }

                // Node needs to be added
                node = new Node(
                        swarmNodeId,
                        getHostname(),
                        this.getMaxHostMemory()
                );

                session.save(node);
                logger.info("Added new node {}.", node);

            } catch (Exception e) {
                tx.rollback();
                throw e;

            } finally {
                tx.commit();
            }

        }

    }

    /**
     * Reads the hostname of the nodes host
     * @return
     */
    private String getHostname() {
        return System.getProperty("HOSTNAME");
    }

    /**
     * Starts the watcher for container starts / deaths
     */
    private void startContainerWatcher() {
        this.containerWatcher = new ContainerWatcher();
        containerWatcher.addNewContainerObserver(this.logFetcher);
        containerWatcher.addRemoveContainerObserver(this.logFetcher);
        containerWatcher.addNewContainerObserver(this.containerResourceWatcher);
        containerWatcher.addNewContainerObserver(this.containerHealthReader);
        containerWatcher.addRemoveContainerObserver(this.containerHealthReader);

        dockerClient.eventsCmd()
                .withLabelFilter(Constants.DOCKER_IDENTIFIER_MAP)
                .withEventTypeFilter(EventType.CONTAINER)
                .withEventFilter("start", "die")
                .exec(this.containerWatcher);
    }

    /**
     * Starts the watcher for the docker hosts resources
     */
    private void startHostResourceMonitor() {
        this.hostResourceWatcherThread = new HostResourceWatcher();
        this.hostResourceWatcherThread.start();
    }

    /**
     * Starts the container log fetcher, which periodically fetches the containers new logs
     */
    private void startContainerLogFetcher() {
        this.logFetcher = new ContainerLogFetcher();
        this.logFetcher.init();
        this.logFetcher.start();
    }

    /**
     * Starts the container stats monitor
     */
    private void startContainerResourceWatcher() {
        this.containerResourceWatcher = new ContainerResourceWatcher();
        this.containerResourceWatcher.init();
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
        this.containerHealthReader.init();
        this.containerHealthReader.start();
    }

    /**
     * Starts the watcher for service starts / stops
     */
    private void startServiceWatcher() {
        this.serviceWatcher = new ServiceWatcher();

        dockerClient.eventsCmd()
                .withEventTypeFilter(EventType.SERVICE)
                .exec(this.serviceWatcher);
    }

    /**
     * Starts the service health reader if the node is a master node and doesn't have the label 'FETCH_SERVICE_HEALTH=false'
     */
    private void startServiceHealthReader() {
        List<SwarmNode> nodes = dockerClient.listSwarmNodesCmd()
                .withIdFilter(Collections.singletonList(swarmNodeId))
                .exec();
        if (nodes.size() != 1) {
            throw new DockerMCException("Docker swarm node %s not found. Found %s nodes.", swarmNodeId, nodes.size());
        }
        SwarmNode node = nodes.get(0);

        Map<String, String> labels = node.getSpec().getLabels();
        if (labels.getOrDefault(Constants.FETCH_SERVICE_HEALTH_NODE_LABEL, "true").equals("false")) {
            logger.warn("Local node {} has label {}=true. Preventing service health readings from this node.", swarmNodeId, Constants.FETCH_SERVICE_HEALTH_NODE_LABEL);
            return;
        }

        this.serviceHealthReader = new ServiceHealthReader();
        this.serviceHealthReader.start();
    }


    @SneakyThrows
    public void start() {
        dockerClient.pingCmd().exec();

        this.saveSwarmNode();
//        this.startHostResourceMonitor();
//        this.startContainerLogFetcher();
//        this.startContainerResourceWatcher();
//        this.startContainerHealthReader();
//        this.startContainerWatcher();   // Start last, since most watchers need its events
//        this.startTimedPermissionWatcher();
        this.startServiceWatcher();

        logger.info("NodeWatcher started successfully.");
    }

    @SneakyThrows
    public void stop() {
        if (hostResourceWatcherThread != null) {
            hostResourceWatcherThread.interrupt();
        }
        if (logFetcher != null) {
            logFetcher.interrupt();
        }
        if (containerWatcher != null) {
            containerWatcher.close();
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
    }


    // -----  Utility methods  -----


    /**
     * Tries to get the name of a swarm node from its labels
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
     * @return
     */
    private Integer getMaxHostMemory() throws InvalidSystemStateException {
        String output = Utils.executeCommandOnHost(new String[]{"cat", "/proc/meminfo"});
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
