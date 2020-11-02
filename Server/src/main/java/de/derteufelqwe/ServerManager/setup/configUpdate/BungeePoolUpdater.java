package de.derteufelqwe.ServerManager.setup.configUpdate;

import com.github.dockerjava.api.model.UpdateConfig;
import com.github.dockerjava.api.model.UpdateFailureAction;
import com.github.dockerjava.api.model.UpdateOrder;
import com.orbitz.consul.KeyValueClient;
import de.derteufelqwe.ServerManager.Docker;
import de.derteufelqwe.ServerManager.setup.ServiceCreateResponse;
import de.derteufelqwe.ServerManager.setup.servers.BungeePool;
import de.derteufelqwe.ServerManager.setup.servers.ServerPool;
import de.derteufelqwe.ServerManager.setup.templates.ServiceTemplate;
import de.derteufelqwe.commons.Constants;

public class BungeePoolUpdater extends DMCServiceUpdater<BungeePool> {

    public BungeePoolUpdater(Docker docker) {
        super(docker);
    }

    @Override
    protected BungeePool getNewConfig() {
        return this.infrastructureConfig.getBungeePool();
    }

    @Override
    protected BungeePool getOldConfig() {
        return this.systemConfig.getBungeePool();
    }

    @Override
    protected void setOldConfig(BungeePool configObj) {
        if (configObj != null) {
            this.systemConfig.setBungeePool(configObj);

        } else {
            this.systemConfig.setBungeePool(null);
        }
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
