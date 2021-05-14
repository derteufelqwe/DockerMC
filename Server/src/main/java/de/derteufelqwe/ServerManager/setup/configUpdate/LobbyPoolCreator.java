package de.derteufelqwe.ServerManager.setup.configUpdate;

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

    public LobbyPoolCreator(Docker docker, JedisPool jedisPool) {
        super(
                ServerManager.getServerConfig().get().getLobbyPool(),
                ServerManager.getServerConfigOld().get().getLobbyPool(),
                docker,
                Constants.ContainerType.MINECRAFT
        );
        this.jedisPool = jedisPool;
    }

    @Override
    protected void updateOldConfigFile(ServerPool newConfig) {
        Config<ServersConfig> serversConfig = ServerManager.getServerConfigOld();
        serversConfig.get().setLobbyPool(newConfig);
        serversConfig.save();
    }

    @Override
    protected int getParallelUpdateCount() {
        MainConfig mainConfig = ServerManager.getMainConfig().get();
        return mainConfig.getLobbyPoolParallelUpdates();
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
