package de.derteufelqwe.ServerManager.setup;

import com.github.dockerjava.api.model.Service;
import com.orbitz.consul.KeyValueClient;
import de.derteufelqwe.ServerManager.Docker;
import de.derteufelqwe.ServerManager.ServerManager;
import de.derteufelqwe.ServerManager.Utils;
import de.derteufelqwe.ServerManager.config.InfrastructureConfig;
import de.derteufelqwe.ServerManager.setup.servers.BungeePool;
import de.derteufelqwe.ServerManager.setup.servers.ServerPool;
import de.derteufelqwe.commons.Constants;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Creates the Minecraft server services based on the config
 */
public class ServerConfigSetup {

    private InfrastructureConfig config = ServerManager.CONFIG.get(InfrastructureConfig.class);
    private Docker docker;
    private KeyValueClient kvClient;

    private ConfigSetupResponse createResponse;

    public ServerConfigSetup(Docker docker, KeyValueClient kvClient) {
        this.docker = docker;
        this.kvClient = kvClient;
    }


    /**
     * Creates the configured services
     */
    public ConfigSetupResponse setup() {
        this.createResponse = new ConfigSetupResponse();

        this.createBungeePool();
        this.createLobbyPool();
        this.createPoolServers();

        return this.createResponse;
    }

    private List<Service> getRelevantServices() {
        Map<String, String> labels1 = Utils.quickLabel(Constants.ContainerType.BUNGEE_POOL);
        Map<String, String> labels2 = Utils.quickLabel(Constants.ContainerType.MINECRAFT_POOL);
        Map<String, String> labels3 = Utils.quickLabel(Constants.ContainerType.MINECRAFT_POOL_PERSISTENT);

        List<Service> existingServices = this.docker.getDocker().listServicesCmd().withLabelFilter(labels1).exec();
        existingServices.addAll(this.docker.getDocker().listServicesCmd().withLabelFilter(labels2).exec());
        existingServices.addAll(this.docker.getDocker().listServicesCmd().withLabelFilter(labels3).exec());

        return existingServices;
    }

    /**
     * Identifies lost services. A service is lost if it's labeled as a configurable server but not specified by any config.
     *
     * @param response Response of #setup().
     */
    public List<Service> findLostServices(ConfigSetupResponse response) {
        List<Service> existingServices = this.getRelevantServices();
        List<String> configuredServicesNames = response.getResults().stream().map(ServiceCreateResponse::getServiceName).collect(Collectors.toList());

        for (Service service : new ArrayList<>(existingServices)) {
            if (configuredServicesNames.contains(service.getSpec().getName())) {
                existingServices.remove(service);
            }
        }

        return existingServices;
    }


    private void createBungeePool() {
        BungeePool bungeePool = this.config.getBungeePool();
        ServiceCreateResponse response = new ServiceCreateResponse(Constants.ContainerType.BUNGEE_POOL);

        if (bungeePool == null) {
            System.err.println("No bungee service configured.");
            response.setResult(ServiceStart.NOT_CONFIGURED);

        } else {
            bungeePool.init(docker);
            response.setServiceName(bungeePool.getName());

            if (bungeePool.find().isFound()) {
                System.out.println("Found existing BungeeCord service.");
                response.setResult(ServiceStart.RUNNING);

            } else {
                System.out.println("Couldn't find existing bungee service. Creating it...");

                if (bungeePool.create().isCreated()) {
                    System.out.println("Successfully created bungee service.");
                    response.setResult(ServiceStart.OK);

                } else {
                    System.err.println("Failed to create bungee service.");
                    response.setResult(ServiceStart.FAILED_GENERIC);
                }
            }
        }

        this.createResponse.addResult(response);
    }

    /**
     * Sets the default server name in consul.
     *
     * @param serverName Name to set
     */
    private void addToConsul(String serverName) {
        kvClient.putValue("system/lobbyServerName", serverName);
    }

    private void createLobbyPool() {
        ServerPool lobbyPool = this.config.getLobbyPool();
        ServiceCreateResponse response = new ServiceCreateResponse(Constants.ContainerType.MINECRAFT_POOL);

        if (lobbyPool == null) {
            System.err.println("No lobby service configured.");
            response.setResult(ServiceStart.NOT_CONFIGURED);

        } else {
            lobbyPool.init(docker);
            response.setServiceName(lobbyPool.getName());

            if (lobbyPool.find().isFound()) {
                System.out.println("Found existing lobby service.");
                response.setResult(ServiceStart.RUNNING);
                this.addToConsul(lobbyPool.getName());

            } else {
                System.out.println("Couldn't find existing lobby service. Creating it...");

                if (lobbyPool.create().isCreated()) {
                    System.out.println("Successfully created lobby service.");
                    response.setResult(ServiceStart.OK);
                    this.addToConsul(lobbyPool.getName());

                } else {
                    System.err.println("Failed to create lobby service.");
                    response.setResult(ServiceStart.FAILED_GENERIC);
                }
            }
        }

        this.createResponse.addResult(response);
    }

    private void createPoolServers() {
        for (ServerPool pool : this.config.getPoolServers()) {
            pool.init(docker);
            ServiceCreateResponse response = new ServiceCreateResponse(pool.getName(), Constants.ContainerType.MINECRAFT_POOL);

            if (pool.find().isFound()) {
                System.out.println(String.format("Found existing server pool service %s.", pool.getName()));
                response.setResult(ServiceStart.RUNNING);

            } else {
                System.out.println(String.format("Couldn't find existing pool server service %s. Creating it...", pool.getName()));

                if (pool.create().isCreated()) {
                    System.out.println(String.format("Successfully created pool server service %s.", pool.getName()));
                    response.setResult(ServiceStart.OK);

                } else {
                    System.err.println(String.format("Failed to create pool server service %s.", pool.getName()));
                    response.setResult(ServiceStart.FAILED_GENERIC);
                }
            }

            this.createResponse.addResult(response);
        }
    }

}
