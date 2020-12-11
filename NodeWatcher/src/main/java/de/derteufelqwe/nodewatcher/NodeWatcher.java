package de.derteufelqwe.nodewatcher;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.model.Info;
import com.github.dockerjava.api.model.SwarmNode;
import com.github.dockerjava.api.model.SwarmNodeSpec;
import com.github.dockerjava.core.DefaultDockerClientConfig;
import com.github.dockerjava.core.DockerClientConfig;
import com.github.dockerjava.core.DockerClientImpl;
import com.github.dockerjava.httpclient5.ApacheDockerHttpClient;
import com.github.dockerjava.transport.DockerHttpClient;
import de.derteufelqwe.commons.Utils;
import de.derteufelqwe.commons.hibernate.SessionBuilder;
import de.derteufelqwe.commons.hibernate.objects.Node;
import lombok.Getter;
import lombok.SneakyThrows;
import org.hibernate.Session;
import org.hibernate.Transaction;

import javax.annotation.CheckForNull;
import javax.persistence.criteria.CriteriaBuilder;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Docker nodes should have a name label
 */
public class NodeWatcher {

    private final Pattern RE_MEM_TOTAL = Pattern.compile("MemTotal:\\s+(\\d+).+");
    private final String CONTAINER_FILTER = "Owner=DockerMC";
    private final String EVENT_TYPE = "die";

    @Getter
    private static DockerClient dockerClient;
    @Getter
    private static SessionBuilder sessionBuilder;
    @Getter
    private static String swarmNodeId;

    private final String dockerHost;
    private final String postgresHost;

    private HostResourceWatcher hostResourceWatcherThread;


    public NodeWatcher(String dockerHost, String postgresHost) {
        this.dockerHost = dockerHost;
        this.postgresHost = postgresHost;
        dockerClient = this.createDockerClient();
        sessionBuilder = this.createSessionBuilder();
        swarmNodeId = this.getLocalSwarmNodeId();
    }


    private DockerClientConfig getDockerClientConfig() {
        return DefaultDockerClientConfig.createDefaultConfigBuilder()
                .withDockerHost(dockerHost)
                .withDockerTlsVerify(false)
                .withApiVersion("1.40")
                .build();
    }

    private DockerHttpClient getDockerHttpClient() {
        DockerClientConfig clientConfig = this.getDockerClientConfig();

        return new ApacheDockerHttpClient.Builder()
                .dockerHost(clientConfig.getDockerHost())
                .sslConfig(clientConfig.getSSLConfig())
                .build();
    }

    private DockerClient createDockerClient() {
        return DockerClientImpl.getInstance(this.getDockerClientConfig(), this.getDockerHttpClient());
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
    private void saveSwarmNodes() {
        List<SwarmNode> nodes = dockerClient.listSwarmNodesCmd()
                .withIdFilter(Collections.singletonList(swarmNodeId))
                .exec();

        if (nodes.size() != 1) {
            System.err.printf("Found %s node: %s.\n", nodes.size(), nodes);
            System.exit(600);
        }

        try (Session session = sessionBuilder.openSession()) {
            SwarmNode swarmNode = nodes.get(0);
            Transaction tx = session.beginTransaction();

            try {
                Node node = session.get(Node.class, swarmNode.getId());

                // Node already known
                if (node != null) {
                    return;
                }

                // Node needs to be added
                node = new Node(swarmNode.getId(), this.getSwarmName(swarmNode));
                node.setMaxRam(this.getMaxHostMemory());

                session.save(node);
                System.out.println("Added new node " + node);

            } finally {
                tx.commit();
            }

        }

    }

    /**
     * Starts the watcher for container starts / deaths
     */
    private void startContainerWatcher() {
        dockerClient.eventsCmd()
                .withLabelFilter(CONTAINER_FILTER)
                .withEventFilter("start", "die")
                .exec(new ContainerWatcher());
    }


    private void startHostResourceMonitor() {
        this.hostResourceWatcherThread = new HostResourceWatcher();
        this.hostResourceWatcherThread.start();
    }


    public void start() {
        dockerClient.pingCmd().exec();

        this.saveSwarmNodes();
//        this.startHostResourceMonitor();
        this.startContainerWatcher();

    }

    @SneakyThrows
    public void stop() {
        if (dockerClient != null) {
            dockerClient.close();
        }
        if (hostResourceWatcherThread != null) {
            hostResourceWatcherThread.interrupt();
        }
    }

}
