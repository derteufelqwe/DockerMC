package de.derteufelqwe.bungeeplugin;

import com.google.protobuf.ByteString;
import com.ibm.etcd.api.Event;
import com.ibm.etcd.api.KeyValue;
import com.ibm.etcd.api.RangeResponse;
import com.ibm.etcd.client.EtcdClient;
import com.ibm.etcd.client.KvStoreClient;
import com.ibm.etcd.client.kv.KvClient;
import com.ibm.etcd.client.kv.WatchUpdate;
import de.derteufelqwe.commons.Constants;
import io.grpc.stub.StreamObserver;
import lombok.SneakyThrows;

import java.net.ConnectException;
import java.util.*;
import java.util.concurrent.TimeUnit;

public class DockerPoolHandler extends Thread {

    private final ByteString CLIENTS_KEY = Utils.toBs(Constants.ETCD_KEY);

    private KvStoreClient kvStoreClient;
    private KvClient client;
    private Map<String, DockerConfig> dockerInstances = Collections.synchronizedMap(new HashMap<>());
    private Map<String, Pair<String, String>> addMap = new HashMap<>();
    private Map<String, Pair<String, String>> removeMap = new HashMap<>();


    public DockerPoolHandler() {
        this.kvStoreClient = EtcdClient.forEndpoint(Constants.ETCD_CONTAINER_NAME, Constants.ETCD_PORT)
                .withPlainText()
                .build();
        this.client = kvStoreClient.getKvClient();
    }


    @SneakyThrows
    @Override
    public void run() {
        getExistingEntrys();

        startWatch();

        // Prevents this thread from exiting because the watch is async
        while (true) {
            TimeUnit.SECONDS.sleep(1);
        }
    }

    /**
     * Finds and adds existing nodes
     */
    private void getExistingEntrys() {
        List<KeyValue> keys = this.client.get(Utils.toBs(Constants.ETCD_KEY)).asPrefix().sync().getKvsList();
        Set<String> containerSet = new HashSet<>();

        // Get all registered container names
        for (KeyValue kv : keys) {
            String key = Utils.fromBs(kv.getKey());
            String[] splitKey = key.split("/");

            if (splitKey.length < 3) {
                return;
            }

            String containerName = splitKey[1];

            containerSet.add(containerName);
        }

        // Add the nodes based on the containerSet
        for (String containerName : containerSet) {
            List<KeyValue> ipKvs = this.client.get(Utils.toBs(Constants.ETCD_KEY + "/" + containerName + "/ip")).sync().getKvsList();
            List<KeyValue> nameKvs = this.client.get(Utils.toBs(Constants.ETCD_KEY + "/" + containerName + "/name")).sync().getKvsList();
            String ip = null;
            String name = null;

            // Set ip
            if (ipKvs.size() == 1) {
                ip = Utils.fromBs(ipKvs.get(0).getValue());

            } else if (ipKvs.size() < 1) {
                System.err.println("[Warning] No value for ip for container " + containerName + " found.");

            } else {
                System.err.println("[Warning] Multiple entries found for ip for container " + containerName + ".");
            }

            // Set name
            if (nameKvs.size() == 1) {
                name = Utils.fromBs(nameKvs.get(0).getValue());

            } else if (nameKvs.size() < 1) {
                System.err.println("[Warning] No value for name for container " + containerName + " found.");

            } else {
                System.err.println("[Warning] Multiple entries found for name for container " + containerName + ".");
            }

            // Finally add the node
            if (ip != null && name != null) {
                this.addNode(containerName, ip, name);
            }

        }

    }

    /**
     * Starts the watching process of the clients section of etcd.
     */
    public void startWatch() {

        StreamObserver<WatchUpdate> observer = new StreamObserver<WatchUpdate>() {
            @Override
            public void onNext(WatchUpdate value) {
                for (Event event : value.getEvents()) {
                    processEvent(event);
                }
            }

            @Override
            public void onError(Throwable t) {
                t.printStackTrace();
            }

            @Override
            public void onCompleted() {
                System.out.println("Done");
            }
        };

        KvClient.Watch watch = this.client.watch(CLIENTS_KEY).asPrefix().start(observer);
    }

    /**
     * Processes an event to add and remove nodes
     * @param event
     */
    private void processEvent(Event event) {
        Event.EventType type = event.getType();
        String key = Utils.fromBs(event.getKv().getKey());
        String value = Utils.fromBs(event.getKv().getValue());
        String[] splitKey = key.split("/");

        if (splitKey.length < 3) {
            return;
        }

        String prefix = splitKey[0];
        String containerName = splitKey[1];
        String dataKey = splitKey[2];

        if (!prefix.equals(Constants.ETCD_KEY)) {
            return;
        }

        // Handle addition of Nodes
        if (type == Event.EventType.PUT) {
            Pair<String, String> nodeData = this.addMap.get(containerName) != null ? this.addMap.get(containerName) : new Pair<>();
            if (dataKey.equals("ip")) {
                nodeData.first = value;

            } else if (dataKey.equals("name")) {
                nodeData.second = value;

            } else {
                System.out.println("[Warning] Added unknown key " + dataKey + " to " + containerName + ".");
            }

            this.addMap.put(containerName, nodeData);

            // Process data when full
            if (nodeData.first != null && nodeData.second != null) {
                this.addMap.remove(containerName);
                this.addNode(containerName, nodeData.first, nodeData.second);
            }

        // Handle removal of nodes
        } else if (type == Event.EventType.DELETE) {
            Pair<String, String> nodeData = this.removeMap.get(containerName) != null ? this.removeMap.get(containerName) : new Pair<>();
            if (dataKey.equals("ip")) {
                nodeData.first = value;

            } else if (dataKey.equals("name")) {
                nodeData.second = value;

            } else {
                System.out.println("[Warning] Added unknown key " + dataKey + " to " + containerName + ".");
            }

            this.removeMap.put(containerName, nodeData);

            // Process data when full
            if (nodeData.first != null && nodeData.second != null) {
                this.removeMap.remove(containerName);
                this.removeNode(containerName, nodeData.first, nodeData.second);
            }

        // Handle unknown operations
        } else {
            System.out.println("Got unsupported Event " + event.getType());
        }
    }

    @SneakyThrows
    private void addNode(String container, String ip, String name) {
        System.out.println("Adding node container " + container);

        Docker dockerInst = null;
        // Try to connect asap when the container is up and running
        for (int i = 0; i < 10; i++) {
            try {
                dockerInst = new Docker(ip, 80);
                dockerInst.getDocker().listContainersCmd().exec();
                break;

            } catch (Exception e) {
                dockerInst = null;
                TimeUnit.SECONDS.sleep(1);
            }
        }

        if (dockerInst == null) {
            throw new RuntimeException("Failed to connect to docker api container. Retried 10 times.");
        }

        Thread eventHandler = new DockerEventHandler(dockerInst);
        eventHandler.start();

        DockerConfig dockerConfig = new DockerConfig();
            dockerConfig.setName(container);
            dockerConfig.setHost(ip);
            dockerConfig.setPort(80);
            dockerConfig.setDockerInst(dockerInst);
            dockerConfig.setThread(eventHandler);

        this.dockerInstances.put(container, dockerConfig);
    }

    @SneakyThrows
    private void removeNode(String container, String ip, String name) {
        System.out.println("Removing node container " + container);

        DockerConfig dockerConfig = this.dockerInstances.get(container);
        Thread eventhHandler = dockerConfig.getThread();
        eventhHandler.interrupt();

        for (int i = 0; i < 10; i++) {
            if (eventhHandler.isAlive()) {
                TimeUnit.SECONDS.sleep(1);

            } else {
                continue;
            }
        }

        if (eventhHandler.isAlive()) {
            throw new RuntimeException("Failed to kill eventhandler for container " + container + ".");
        }

        this.dockerInstances.remove(container);
    }


}
