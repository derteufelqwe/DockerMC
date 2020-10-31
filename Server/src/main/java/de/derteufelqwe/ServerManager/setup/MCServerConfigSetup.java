package de.derteufelqwe.ServerManager.setup;

import com.orbitz.consul.KeyValueClient;
import de.derteufelqwe.ServerManager.Docker;
import de.derteufelqwe.ServerManager.ServerManager;
import de.derteufelqwe.ServerManager.config.InfrastructureConfig;
import de.derteufelqwe.ServerManager.config.SystemConfig;
import de.derteufelqwe.ServerManager.setup.servers.BungeePool;
import de.derteufelqwe.ServerManager.setup.servers.ServerPool;
import de.derteufelqwe.ServerManager.setup.templates.DockerObjTemplate;
import de.derteufelqwe.commons.Constants;

import java.util.ArrayList;
import java.util.List;

/**
 * Creates the Minecraft server services based on the config
 */
public class MCServerConfigSetup {

    private InfrastructureConfig infrastructureConfig = ServerManager.CONFIG.get(InfrastructureConfig.class);
    private SystemConfig systemConfig = ServerManager.CONFIG.get(SystemConfig.class);
    private Docker docker;
    private KeyValueClient kvClient;
    private boolean updateConfig = true;    // Update the secondary config

    public MCServerConfigSetup(Docker docker, KeyValueClient kvClient) {
        this.docker = docker;
        this.kvClient = kvClient;
    }

    public MCServerConfigSetup(Docker docker, KeyValueClient kvClient, boolean updateConfig) {
        this(docker, kvClient);
        this.updateConfig = updateConfig;
    }


    public ServiceCreateResponse createBungeePool() {
        BungeePool bungeePool = this.infrastructureConfig.getBungeePool();
        ServiceCreateResponse response = new ServiceCreateResponse(Constants.ContainerType.BUNGEE_POOL);

        if (bungeePool == null) {
            response.setResult(ServiceStart.NOT_CONFIGURED);
            return response;
        }


        bungeePool.init(docker);
        response.setServiceName(bungeePool.getName());

        DockerObjTemplate.FindResponse findResponse = bungeePool.find();

        if (findResponse.isFound()) {
            response.setResult(ServiceStart.RUNNING);
            response.setServiceId(findResponse.getServiceID());

        } else {
            DockerObjTemplate.CreateResponse createResponse = bungeePool.create();
            response.setServiceId(createResponse.getServiceID());

            if (createResponse.isCreated()) {
                response.setResult(ServiceStart.OK);
                if (this.updateConfig) {
                    this.systemConfig.setBungeePool((BungeePool) this.infrastructureConfig.getBungeePool().clone());
                }

            } else {
                response.setResult(ServiceStart.FAILED_GENERIC);
                response.setAdditionalInfos(createResponse.getMessage());
            }
        }

        return response;
    }

    public ServiceCreateResponse createLobbyPool() {
        ServerPool lobbyPool = this.infrastructureConfig.getLobbyPool();
        ServiceCreateResponse response = new ServiceCreateResponse(Constants.ContainerType.MINECRAFT_POOL);

        if (lobbyPool == null) {
            response.setResult(ServiceStart.NOT_CONFIGURED);
            return response;
        }


        lobbyPool.init(docker);
        response.setServiceName(lobbyPool.getName());

        if (lobbyPool.find().isFound()) {
            response.setResult(ServiceStart.RUNNING);
            this.addToConsul(lobbyPool.getName());

        } else {
            DockerObjTemplate.CreateResponse createResponse = lobbyPool.create();

            if (createResponse.isCreated()) {
                response.setResult(ServiceStart.OK);
                this.addToConsul(lobbyPool.getName());
                if (this.updateConfig) {
                    this.systemConfig.setLobbyPool((ServerPool) this.infrastructureConfig.getLobbyPool().clone());
                }

            } else {
                response.setResult(ServiceStart.FAILED_GENERIC);
                response.setAdditionalInfos(createResponse.getMessage());
            }

        }

        return response;
    }

    public List<ServiceCreateResponse> createPoolServers() {
        List<ServiceCreateResponse> responses = new ArrayList<>();
        this.systemConfig.getPoolServers().clear();

        for (ServerPool pool : this.infrastructureConfig.getPoolServers()) {
            pool.init(docker);
            ServiceCreateResponse response = new ServiceCreateResponse(pool.getName(), Constants.ContainerType.MINECRAFT_POOL);

            if (pool.find().isFound()) {
                response.setResult(ServiceStart.RUNNING);

            } else {
                DockerObjTemplate.CreateResponse createResponse = pool.create();

                if (createResponse.isCreated()) {
                    response.setResult(ServiceStart.OK);
                    if (this.updateConfig) {
                        this.systemConfig.getPoolServers().add(pool);
                    }

                } else {
                    response.setResult(ServiceStart.FAILED_GENERIC);
                    response.setAdditionalInfos(createResponse.getMessage());
                }
            }

            responses.add(response);
        }

        return responses;
    }

    /**
     * Sets the default server name in consul.
     *
     * @param serverName Name to set
     */
    private void addToConsul(String serverName) {
        kvClient.putValue("system/lobbyServerName", serverName);
    }

}
