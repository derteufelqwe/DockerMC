package de.derteufelqwe.ServerManager.setup.configUpdate;

import com.github.dockerjava.api.model.UpdateConfig;
import com.github.dockerjava.api.model.UpdateFailureAction;
import com.github.dockerjava.api.model.UpdateOrder;
import de.derteufelqwe.ServerManager.Docker;
import de.derteufelqwe.ServerManager.setup.ServiceCreateResponse;
import de.derteufelqwe.ServerManager.setup.servers.ServerPool;
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
        List<ServerPool> pools = this.systemConfig.getPoolServers().stream().filter(p -> p.getName().equals(this.newConfig.getName())).collect(Collectors.toList());

        if (pools.size() == 1) {
            return pools.get(0);
        }

        return null;
    }

    @Override
    protected void setOldConfig(@Nullable ServerPool configObj) {
        this.systemConfig.getPoolServers().add(configObj);
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

}
