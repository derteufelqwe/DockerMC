package de.derteufelqwe.ServerManager.setup.configUpdate;

import com.orbitz.consul.KeyValueClient;
import de.derteufelqwe.ServerManager.Docker;
import de.derteufelqwe.ServerManager.setup.servers.ServerPool;
import de.derteufelqwe.ServerManager.setup.templates.DockerObjTemplate;
import de.derteufelqwe.commons.Constants;

class LobbyPoolCreator extends DMCServiceCreator<ServerPool> {

    protected KeyValueClient kvClient;

    public LobbyPoolCreator(Docker docker, KeyValueClient kvClient) {
        super(docker);
        this.kvClient =kvClient;
    }


    @Override
    protected ServerPool getConfigObject() {
        return this.infrastructureConfig.getLobbyPool();
    }

    @Override
    protected Constants.ContainerType getContainerType() {
        return Constants.ContainerType.MINECRAFT_POOL;
    }

    @Override
    protected void updateSystemConfig(ServerPool newData) {
        this.systemConfig.setLobbyPool(newData);
    }

    /**
     * Sets the default server name in consul.
     *
     * @param serverName Name to set
     */
    private void addToConsul(String serverName) {
        kvClient.putValue("system/lobbyServerName", serverName);
    }

    @Override
    protected void onServiceCreated(DockerObjTemplate.CreateResponse createResponse) {
        super.onServiceCreated(createResponse);

        this.addToConsul(this.getConfigObject().getName());

    }

}
