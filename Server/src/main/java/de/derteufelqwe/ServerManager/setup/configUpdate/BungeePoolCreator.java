package de.derteufelqwe.ServerManager.setup.configUpdate;

import de.derteufelqwe.ServerManager.Docker;
import de.derteufelqwe.ServerManager.setup.servers.BungeePool;
import de.derteufelqwe.commons.Constants;

class BungeePoolCreator extends DMCServiceCreator<BungeePool> {

    public BungeePoolCreator(Docker docker) {
        super(docker);
    }

    @Override
    protected BungeePool getConfigObject() {
        return this.serversConfig.getBungeePool();
    }

    @Override
    protected Constants.ContainerType getContainerType() {
        return Constants.ContainerType.BUNGEE_POOL;
    }

    @Override
    protected void updateSystemConfig(BungeePool newData) {
        this.systemConfig.setBungeePool(newData);
    }

}
