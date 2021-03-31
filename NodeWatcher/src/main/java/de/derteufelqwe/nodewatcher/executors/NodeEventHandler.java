package de.derteufelqwe.nodewatcher.executors;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.async.ResultCallback;
import com.github.dockerjava.api.model.*;
import de.derteufelqwe.commons.CommonsAPI;
import de.derteufelqwe.commons.hibernate.LocalSessionRunnable;
import de.derteufelqwe.commons.hibernate.SessionBuilder;
import de.derteufelqwe.commons.hibernate.objects.Node;
import de.derteufelqwe.nodewatcher.NodeWatcher;
import lombok.SneakyThrows;
import lombok.extern.log4j.Log4j2;
import org.hibernate.Session;
import org.jetbrains.annotations.NotNull;

import java.io.Closeable;
import java.io.IOException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;

/**
 * Watches for docker service events to update the services in the database.
 */
@Log4j2
public class NodeEventHandler implements ResultCallback<Event> {

    private final DockerClient dockerClient = NodeWatcher.getDockerClientFactory().forceNewDockerClient();
    private final SessionBuilder sessionBuilder = NodeWatcher.getSessionBuilder();

    /**
     * Indicates that the NodeEventHandler was started and all nodes are present in the DB
     */
    private final CountDownLatch hasStartedLatch = new CountDownLatch(1);

    public NodeEventHandler() {

    }


    /**
     * Initializes the NodeEventHandler and makes sure the DB is up-to-date
     */
    @Override
    public void onStart(Closeable closeable) {
        try {
            List<String> runningNodesDB = this.getRunningNodesFromDB();
            List<String> runningNodesDocker = this.getRunningNodesFromDocker();
            this.addNewNodes(runningNodesDB, runningNodesDocker);
            this.stopRemovedNodes(runningNodesDB, runningNodesDocker);

            this.hasStartedLatch.countDown();

        } catch (Exception e) {
            log.error("NodeEventHandler failed to start.", e);
            CommonsAPI.getInstance().createExceptionNotification(sessionBuilder, e, NodeWatcher.getMetaData());
            throw e;
        }
    }

    @Override
    public void onNext(Event object) {
        try {
            EventActor actor = object.getActor();
            if (object.getAction() == null) {
                log.warn("Got event without action: {}.", object);
                return;
            }
            if (actor == null) {
                log.warn("Got event without actor: {}.", object);
                return;
            }
            // create,
            switch (object.getAction()) {
                case "create":
                    this.onNodeCreated(actor.getId());
                    break;

                case "update":
                    this.onNodeUpdated(actor.getId());
                    break;

                case "remove":
                    this.onNodeRemoved(actor.getId(), object.getTimeNano());
                    break;

                default:
                    log.error("NodeEventHandler got invalid event type " + object);
            }

        } catch (Exception e) {
            log.error("Exception occurred in the ContainerWatcher.");
            log.error(e.getMessage());
            e.printStackTrace(System.err);
            CommonsAPI.getInstance().createExceptionNotification(sessionBuilder, e, NodeWatcher.getMetaData());
        }
    }

    @Override
    public void onError(Throwable throwable) {
        log.error("Uncaught exception occurred in the NodeEventHandler.", throwable);
    }

    @Override
    public void onComplete() {
        log.info("NodeEventHandler stopped.");
    }

    @Override
    public void close() throws IOException {

    }

    // -----  Setup methods  -----

    /**
     * Compares the running nodes with the existing nodes and adds all nodes to the DB that are missing
     */
    private void addNewNodes(List<String> dbNodes, List<String> dockerNodes) {
        List<String> nodeIDsToAdd = dockerNodes.stream()
                .filter(n -> !dbNodes.contains(n))
                .collect(Collectors.toList());

        if (nodeIDsToAdd.size() > 0) {
            log.info("{} nodes were added during downtime.", nodeIDsToAdd.size());
        }
        // Add the new nodes to the DB
        for (String nodeID : nodeIDsToAdd) {
            this.createOrUpdateNode(nodeID, "create");
        }
    }

    /**
     * Compares the running nodes with the existing nodes and removed all nodes from the DB that are not running anymore
     */
    private void stopRemovedNodes(List<String> dbNodes, List<String> dockerNodes) {
        List<String> nodeIDsToRemove = dbNodes.stream()
                .filter(n -> !dockerNodes.contains(n))
                .collect(Collectors.toList());

        if (nodeIDsToRemove.size() > 0) {
            log.info("{} nodes were removed during downtime.", nodeIDsToRemove.size());
        }

        // Complete the removed nodes in the DB
        for (String nodeID : nodeIDsToRemove) {
            this.completeNode(nodeID, new Timestamp(System.currentTimeMillis()));
        }
    }

    // -----  Event handler methods  -----

    private void onNodeCreated(String nodeID) {
        this.createOrUpdateNode(nodeID, "create");
    }

    /**
     * This gets called periodically for all nodes every ~25 seconds
     * @param nodeID
     */
    private void onNodeUpdated(String nodeID) {
        this.createOrUpdateNode(nodeID, "update");
    }

    private void onNodeRemoved(String nodeID, long timeNano) {
        this.completeNode(nodeID, new Timestamp(timeNano / 1_000_000));
    }

    /**
     * Creates or updates a node entry in the DB
     * @param eventType Type of the event. create or update
     */
    @SneakyThrows   // Required
    private void createOrUpdateNode(String nodeID, String eventType) {
        SwarmNode swarmNode = this.pollForFullSwarmNode(nodeID);

        try {
            new LocalSessionRunnable(sessionBuilder) {
                @SuppressWarnings("ConstantConditions")
                @Override
                protected void exec(Session session) {
                    int maxRAM = (int) (swarmNode.getDescription().getResources().getMemoryBytes() / 1024 / 1024);
                    float availableCPU = (float) (swarmNode.getDescription().getResources().getNanoCPUs() / 1_000_000_000.0);

                    Node node = new Node(
                            swarmNode.getId(),
                            swarmNode.getDescription().getHostname(),
                            new Timestamp(swarmNode.getCreatedAt().getTime()),
                            swarmNode.getStatus().getAddress(),
                            swarmNode.getSpec().getRole() == SwarmNodeRole.MANAGER,
                            maxRAM,
                            availableCPU
                    );

                    session.saveOrUpdate(node);
                }
            }.run();

            if (eventType.equals("create")) {
                log.info("Created node {}.", nodeID);

            } else {
                log.debug("Updated node {}.", nodeID);
            }

        } catch (NullPointerException e) {
            log.error("Creating node {} failed. Some required attributes are null.", nodeID);
            log.trace("Exception: ", e);
        }

    }

    /**
     * Completes a node entry in the DB.
     * @param nodeID
     * @param stopTime
     */
    private void completeNode(String nodeID, Timestamp stopTime) {
        new LocalSessionRunnable(sessionBuilder) {
            @Override
            protected void exec(Session session) {
                Node node = session.get(Node.class, nodeID);
                if (node == null) {
                    log.error("Removing node failed. No node {} found.", nodeID);
                    return;
                }

                node.setLeaveTime(stopTime);

                session.update(node);
            }
        }.run();

        log.info("Completed removed node {}.", nodeID);
    }

    // -----  Utility methods  -----

    public boolean awaitStarted(long timeout, TimeUnit unit) throws InterruptedException {
        return this.hasStartedLatch.await(timeout, unit);
    }

    /**
     * Returns a list of running Node IDs, from the DB.
     * @return
     */
    @NotNull
    private List<String> getRunningNodesFromDB() {
        final List<String> nodeIDs = new ArrayList<>();

        new LocalSessionRunnable(sessionBuilder) {
            @SuppressWarnings("unchecked")
            @Override
            protected void exec(Session session) {
                List<String> existingNodeIDs = (List<String>) session.createNativeQuery(
                        "SELECT n.id FROM nodes AS n WHERE n.leavetime IS NULL"
                ).getResultList();

                nodeIDs.addAll(existingNodeIDs);
            }
        }.run();

        return nodeIDs;
    }

    /**
     * Returns a list of running Node IDs from docker
     * @return
     */
    @NotNull
    private List<String> getRunningNodesFromDocker() {
        return dockerClient.listSwarmNodesCmd()
                .exec().stream()
                .map(SwarmNode::getId)
                .collect(Collectors.toList());
    }

    /**
     * Docker needs a few seconds before the engine returns a SwarmNode with all required information after the node created
     * event was fired.
     * @param nodeID
     * @return
     */
    @NotNull
    private SwarmNode pollForFullSwarmNode(String nodeID) throws TimeoutException {
        long tStart = System.currentTimeMillis();

        while ((System.currentTimeMillis() - tStart) < 10000) {
            List<SwarmNode> swarmNodes = dockerClient.listSwarmNodesCmd()
                    .withIdFilter(Collections.singletonList(nodeID))
                    .exec();

            if (swarmNodes.size() == 0) {
                log.error("Failed to find swarm node {}.", nodeID);

            } else {
                SwarmNode swarmNode = swarmNodes.get(0);
                if (swarmNode.getStatus().getState() != SwarmNodeState.UNKNOWN) {
                    return swarmNode;
                }
            }

            try {
                TimeUnit.MILLISECONDS.sleep(200);

            } catch (InterruptedException e) {
                log.error("Polling for full swarm node got interrupted.");
            }
        }

        throw new TimeoutException("Failed to poll for full swarm node " + nodeID);
    }

}
