package de.derteufelqwe.ServerManager.setup.configUpdate;

import com.github.dockerjava.api.model.UpdateConfig;
import com.github.dockerjava.api.model.UpdateFailureAction;
import com.github.dockerjava.api.model.UpdateOrder;
import com.orbitz.consul.KeyValueClient;
import com.sun.istack.internal.NotNull;
import de.derteufelqwe.ServerManager.Docker;
import de.derteufelqwe.ServerManager.setup.ServiceCreateResponse;
import de.derteufelqwe.ServerManager.setup.servers.ServerPool;
import de.derteufelqwe.commons.Constants;

import javax.annotation.Nullable;

public class LobbyPoolUpdater extends DMCServiceUpdater<ServerPool> {

    private KeyValueClient kvClient;

    public LobbyPoolUpdater(Docker docker, KeyValueClient kvClient) {
        super(docker);
        this.kvClient = kvClient;
    }

    @Override
    protected ServerPool getNewConfig() {
        return this.infrastructureConfig.getLobbyPool();
    }

    @Override
    protected ServerPool getOldConfig() {
        return this.systemConfig.getLobbyPool();
    }

    @Override
    protected void setOldConfig(@Nullable ServerPool configObj) {
        this.systemConfig.setLobbyPool(configObj);
    }

    @Override
    protected Constants.ContainerType getContainerType() {
        return Constants.ContainerType.MINECRAFT_POOL;
    }

    @Override
    protected ServiceCreateResponse createNewService() {
        return new LobbyPoolCreator(this.docker, this.kvClient).create();
    }

    @Override
    protected UpdateConfig getUpdateConfig() {
        return new UpdateConfig()
                .withParallelism(2)
                .withOrder(UpdateOrder.START_FIRST)
                .withFailureAction(UpdateFailureAction.CONTINUE)
                ;
    }

}
