package de.derteufelqwe.ServerManager.setup.configUpdate;

import com.google.inject.Inject;
import com.google.inject.name.Named;
import de.derteufelqwe.ServerManager.Docker;
import de.derteufelqwe.ServerManager.ServerManager;
import de.derteufelqwe.ServerManager.config.MainConfig;
import de.derteufelqwe.ServerManager.config.ServersConfig;
import de.derteufelqwe.ServerManager.setup.ConfigCreator;
import de.derteufelqwe.ServerManager.setup.ServiceCreateResponse;
import de.derteufelqwe.ServerManager.setup.ServiceStart;
import de.derteufelqwe.ServerManager.setup.servers.BungeePool;
import de.derteufelqwe.ServerManager.setup.servers.ServerPool;
import de.derteufelqwe.commons.Constants;
import de.derteufelqwe.commons.config.Config;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;

public class LobbyPoolCreator extends ConfigCreator<ServerPool> {

    private JedisPool jedisPool;

    @Inject
    public LobbyPoolCreator(Docker docker, Config<MainConfig> mainConfig, @Named("current") Config<ServersConfig> serversConfig,
                             @Named("old") Config<ServersConfig> serversConfigOld, JedisPool jedisPool) {
        super(mainConfig,
                serversConfig,
                serversConfigOld,
                serversConfig.get().getLobbyPool(),
                serversConfigOld.get().getLobbyPool(),
                docker,
                Constants.ContainerType.MINECRAFT
        );
        this.jedisPool = jedisPool;
    }

    @Override
    protected void updateOldConfigFile(ServerPool newConfig) {
        serversConfigOld.get().setLobbyPool(newConfig);
        serversConfigOld.save();
    }

    @Override
    protected int getParallelUpdateCount() {
        return mainConfig.get().getLobbyPoolParallelUpdates();
    }

    @Override
    public ServiceCreateResponse createOrUpdate(boolean force) {
        ServiceCreateResponse response = super.createOrUpdate(force);

        if (response.getResult() == ServiceStart.CREATED) {
            try (Jedis jedis = jedisPool.getResource()) {
                jedis.set(Constants.REDIS_KEY_LOBBYSERVER, this.poolConfig.getName());
            }

        } else if (response.getResult() == ServiceStart.DESTROYED) {
            try (Jedis jedis = jedisPool.getResource()) {
                jedis.del(Constants.REDIS_KEY_LOBBYSERVER);
            }
        }

        return response;
    }
}
