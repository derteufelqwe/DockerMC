package de.derteufelqwe.ServerManager.setup.configUpdate;

import com.github.dockerjava.api.model.UpdateConfig;
import com.github.dockerjava.api.model.UpdateFailureAction;
import com.github.dockerjava.api.model.UpdateOrder;
import com.sun.istack.internal.NotNull;
import de.derteufelqwe.ServerManager.Docker;
import de.derteufelqwe.ServerManager.setup.ServiceCreateResponse;
import de.derteufelqwe.ServerManager.setup.ServiceUpdateResponse;
import de.derteufelqwe.ServerManager.setup.servers.ServerPool;
import de.derteufelqwe.ServerManager.setup.templates.DockerObjTemplate;
import de.derteufelqwe.commons.Constants;

import javax.annotation.Nullable;
import java.util.List;
import java.util.stream.Collectors;

public class MinecraftPoolUpdater extends DMCServiceUpdater<ServerPool> {

    private ServerPool newConfig;

    public MinecraftPoolUpdater(Docker docker, ServerPool newConfig) {
        super(docker);
        this.newConfig = newConfig;
    }


    @Override
    protected ServerPool getNewConfig() {
        return this.newConfig;
    }

    @Override
    protected ServerPool getOldConfig() {
        return this.systemConfig.getPoolServers().getServer(this.newConfig.getName());
    }

    @Override
    protected void setOldConfig(@Nullable ServerPool configObj) {
        this.systemConfig.getPoolServers().addServer(configObj);
    }

    @Override
    protected Constants.ContainerType getContainerType() {
        return Constants.ContainerType.MINECRAFT_POOL;
    }

    @Override
    protected ServiceCreateResponse createNewService() {
        return new MinecraftPoolCreator(this.docker, this.newConfig).create();
    }

    @Override
    protected UpdateConfig getUpdateConfig() {
        return new UpdateConfig()
                .withParallelism(2)
                .withOrder(UpdateOrder.START_FIRST)
                .withFailureAction(UpdateFailureAction.CONTINUE)
                ;
    }


    @Override
    public ServiceUpdateResponse update(boolean force) {
        ServiceUpdateResponse response = super.update(force);

        this.systemConfig.getPoolServers().cleanup();

        return response;
    }

}
