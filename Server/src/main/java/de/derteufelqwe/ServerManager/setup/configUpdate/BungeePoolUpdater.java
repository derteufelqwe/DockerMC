package de.derteufelqwe.ServerManager.setup.configUpdate;

import com.github.dockerjava.api.model.UpdateConfig;
import com.github.dockerjava.api.model.UpdateFailureAction;
import com.github.dockerjava.api.model.UpdateOrder;
import de.derteufelqwe.ServerManager.Docker;
import de.derteufelqwe.ServerManager.setup.ServiceCreateResponse;
import de.derteufelqwe.ServerManager.setup.servers.BungeePool;
import de.derteufelqwe.commons.Constants;

import javax.annotation.Nullable;

public class BungeePoolUpdater extends DMCServiceUpdater<BungeePool> {

    public BungeePoolUpdater(Docker docker) {
        super(docker);
    }

    @Override
    protected BungeePool getNewConfig() {
        return this.serversConfig.getBungeePool();
    }

    @Override
    protected BungeePool getOldConfig() {
        return this.oldServersConfig.getBungeePool();
    }

    @Override
    protected void setOldConfig(@Nullable BungeePool configObj) {
        this.oldServersConfig.setBungeePool(configObj);
    }

    @Override
    protected Constants.ContainerType getContainerType() {
        return Constants.ContainerType.BUNGEE_POOL;
    }

    @Override
    protected ServiceCreateResponse createNewService() {
        return new BungeePoolCreator(this.docker).create();
    }

    @Override
    protected UpdateConfig getUpdateConfig() {
        return new UpdateConfig()
                .withParallelism(1)
                .withOrder(UpdateOrder.STOP_FIRST)
                .withFailureAction(UpdateFailureAction.CONTINUE)
                ;
    }
}
