package de.derteufelqwe.nodewatcher;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.model.Info;
import com.github.dockerjava.api.model.SwarmNode;
import com.github.dockerjava.api.model.SwarmNodeSpec;
import de.derteufelqwe.commons.Utils;
import de.derteufelqwe.commons.hibernate.SessionBuilder;
import de.derteufelqwe.commons.hibernate.objects.Node;
import de.derteufelqwe.nodewatcher.logs.ContainerLogFetcher;
import de.derteufelqwe.nodewatcher.misc.DockerClientFactory;
import de.derteufelqwe.nodewatcher.misc.InvalidSystemStateException;
import de.derteufelqwe.nodewatcher.stats.ContainerResourceWatcher;
import de.derteufelqwe.nodewatcher.stats.HostResourceWatcher;
import lombok.Getter;
import lombok.SneakyThrows;
import org.hibernate.Session;
import org.hibernate.Transaction;

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
public class NodeWatcher {

    // ToDo: solve the docker client / thread problems

    private final Pattern RE_MEM_TOTAL = Pattern.compile("MemTotal:\\s+(\\d+).+");
    private final String CONTAINER_FILTER = "Owner=DockerMC";
    private final String EVENT_TYPE = "die";

    @Getter
    private static DockerClientFactory dockerClientFactory;
    @Getter
    private static SessionBuilder sessionBuilder;
    @Getter
    private static String swarmNodeId;

    private final String postgresHost;
    private DockerClient dockerClient;

    // --- Executors ---
    private HostResourceWatcher hostResourceWatcherThread;
    private ContainerWatcher containerWatcher;
    private ContainerLogFetcher logFetcher;
    private ContainerResourceWatcher containerResourceWatcher;


    public NodeWatcher(String dockerHost, String postgresHost) {
        this.postgresHost = postgresHost;
        NodeWatcher.dockerClientFactory = new DockerClientFactory(dockerHost);
        this.dockerClient = dockerClientFactory.getDockerClient();
        sessionBuilder = this.createSessionBuilder();
        swarmNodeId = this.getLocalSwarmNodeId();
    }


    private SessionBuilder createSessionBuilder() {
        return new SessionBuilder("admin", "password", this.postgresHost, false);
    }


    /**
     * Tries to get the name of a swarm node from its labels
     * @param swarmNode
     * @return
     */
    @CheckForNull
    private String getSwarmName(SwarmNode swarmNode) {
        java.lang.String name = null;

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
    private Integer getMaxHostMemory() {
        String output = Utils.executeCommandOnHost(new String[]{"cat", "/proc/meminfo"});
        Matcher m = RE_MEM_TOTAL.matcher(output);

        if (m.find()) {
            try {
                return Integer.parseInt(m.group(1));

            } catch (NumberFormatException e) {
                System.err.println("Read invalid meminfo " + m.group(1) + ".");
                return -2;
            }
        }

        return -1;
    }

    /**
     * Returns the id of the docker swarm node, where this application runs on
     * @return
     */
    private String getLocalSwarmNodeId() {
        Info info = dockerClient.infoCmd().exec();

        return info.getSwarm().getNodeID();
    }


    /**
     * Saves the docker swarm nodes in the database
     */
    private void saveSwarmNode() {
        List<SwarmNode> nodes = dockerClient.listSwarmNodesCmd()
                .withIdFilter(Collections.singletonList(swarmNodeId))
                .exec();

        if (nodes.size() != 1) {
            throw new InvalidSystemStateException("Found %s nodes: %s.\n", nodes.size(), nodes);
        }

        try (Session session = sessionBuilder.openSession()) {
            SwarmNode swarmNode = nodes.get(0);
            Transaction tx = session.beginTransaction();

            try {
                Node node = session.get(Node.class, swarmNode.getId());

                // Node already known
                if (node != null) {
                    System.out.println("[NodeWatcher] Local node already known.");
                    return;
                }

                // Node needs to be added
                node = new Node(swarmNode.getId(), this.getSwarmName(swarmNode));
                node.setMaxRam(this.getMaxHostMemory());

                session.save(node);
                System.out.println("[NodeWatcher] Added new node " + node);

            } finally {
                tx.commit();
            }

        }

    }

    /**
     * Starts the watcher for container starts / deaths
     */
    private void startContainerWatcher() {
        this.containerWatcher = dockerClient.eventsCmd()
                .withLabelFilter(CONTAINER_FILTER)
                .withEventFilter("start", "die")
                .exec(new ContainerWatcher());
        this.containerWatcher.addNewContainerObserver(this.logFetcher);
        this.containerWatcher.addNewContainerObserver(this.containerResourceWatcher);

        System.out.println("init start");
        this.containerWatcher.init();
        System.out.println("init ende");
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


    @SneakyThrows
    public void start() {
        dockerClient.pingCmd().exec();

        this.saveSwarmNode();
        this.startHostResourceMonitor();
        this.startContainerLogFetcher();
        this.startContainerResourceWatcher();
        this.startContainerWatcher();   // Start last, since most watchers need its event

        System.out.println("[NodeWatcher] Started successfully.");
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

        dockerClient.close();
        sessionBuilder.close();
    }

}
