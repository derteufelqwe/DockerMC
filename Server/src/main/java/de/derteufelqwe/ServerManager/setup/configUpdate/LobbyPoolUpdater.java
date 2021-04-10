package de.derteufelqwe.ServerManager.setup.configUpdate;

import com.github.dockerjava.api.model.UpdateConfig;
import com.github.dockerjava.api.model.UpdateFailureAction;
import de.derteufelqwe.ServerManager.Docker;
import de.derteufelqwe.ServerManager.setup.ServiceCreateResponse;
import de.derteufelqwe.ServerManager.setup.servers.ServerPool;
import de.derteufelqwe.commons.Constants;
import redis.clients.jedis.JedisPool;

import javax.annotation.Nullable;

public class LobbyPoolUpdater extends DMCServiceUpdater<ServerPool> {

    private JedisPool jedisPool;

    public LobbyPoolUpdater(Docker docker, JedisPool jedisPool) {
        super(docker);
        this.jedisPool = jedisPool;
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
        return new LobbyPoolCreator(this.docker, this.jedisPool).create();
    }

    @Override
    protected UpdateConfig getUpdateConfig() {
        return new UpdateConfig()
                .withParallelism(mainConfig.get().getLobbyPoolParallelUpdates())
                .withOrder(this.getUpdateOrder())
                .withFailureAction(UpdateFailureAction.CONTINUE)
                ;
    }

}
