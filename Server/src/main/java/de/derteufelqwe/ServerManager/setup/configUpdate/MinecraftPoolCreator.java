package de.derteufelqwe.ServerManager.setup.configUpdate;

import de.derteufelqwe.ServerManager.Docker;
import de.derteufelqwe.ServerManager.setup.servers.ServerPool;
import de.derteufelqwe.commons.Constants;

class MinecraftPoolCreator extends DMCServiceCreator<ServerPool> {

    private ServerPool serverPool;

    public MinecraftPoolCreator(Docker docker, ServerPool serverPool) {
        super(docker);
        this.serverPool = serverPool;
    }

    @Override
    protected ServerPool getConfigObject() {
        return this.serverPool;
    }

    @Override
    protected Constants.ContainerType getContainerType() {
        return Constants.ContainerType.MINECRAFT_POOL;
    }

    @Override
    protected void updateSystemConfig(ServerPool newData) {
        this.systemConfig.getPoolServers().addServer(newData);
    }

}
