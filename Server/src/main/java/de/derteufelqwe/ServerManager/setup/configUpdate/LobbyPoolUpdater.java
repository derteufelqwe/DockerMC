package de.derteufelqwe.ServerManager.setup.configUpdate;

import com.github.dockerjava.api.model.UpdateConfig;
import com.github.dockerjava.api.model.UpdateFailureAction;
import com.github.dockerjava.api.model.UpdateOrder;
import de.derteufelqwe.ServerManager.Docker;
import de.derteufelqwe.ServerManager.setup.ServiceCreateResponse;
import de.derteufelqwe.ServerManager.setup.servers.ServerPool;
import de.derteufelqwe.commons.Constants;
import org.springframework.data.redis.core.StringRedisTemplate;

import javax.annotation.Nullable;

public class LobbyPoolUpdater extends DMCServiceUpdater<ServerPool> {

    private StringRedisTemplate redisTemplate;

    public LobbyPoolUpdater(Docker docker, StringRedisTemplate redisTemplate) {
        super(docker);
        this.redisTemplate = redisTemplate;
    }

    @Override
    protected ServerPool getNewConfig() {
        return this.serversConfig.getLobbyPool();
    }

    @Override
    protected ServerPool getOldConfig() {
        return this.oldServersConfig.getLobbyPool();
    }

    @Override
    protected void setOldConfig(@Nullable ServerPool configObj) {
        this.oldServersConfig.setLobbyPool(configObj);
    }

    @Override
    protected Constants.ContainerType getContainerType() {
        return Constants.ContainerType.MINECRAFT_POOL;
    }

    @Override
    protected ServiceCreateResponse createNewService() {
        return new LobbyPoolCreator(this.docker, this.redisTemplate).create();
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
