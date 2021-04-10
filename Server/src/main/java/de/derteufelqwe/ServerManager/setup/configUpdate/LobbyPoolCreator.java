package de.derteufelqwe.ServerManager.setup.configUpdate;

import de.derteufelqwe.ServerManager.Docker;
import de.derteufelqwe.ServerManager.setup.servers.ServerPool;
import de.derteufelqwe.ServerManager.setup.templates.DockerObjTemplate;
import de.derteufelqwe.commons.Constants;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;

class LobbyPoolCreator extends DMCServiceCreator<ServerPool> {

    protected JedisPool jedisPool;

    public LobbyPoolCreator(Docker docker, JedisPool jedisPool) {
        super(docker);
        this.jedisPool = jedisPool;
    }


    @Override
    protected ServerPool getConfigObject() {
        return this.serversConfig.getLobbyPool();
    }

    @Override
    protected Constants.ContainerType getContainerType() {
        return Constants.ContainerType.MINECRAFT_POOL;
    }

    @Override
    protected void updateSystemConfig(ServerPool newData) {
        this.oldServersConfig.setLobbyPool(newData);
    }

    /**
     * Sets the default server name in redis.
     *
     * @param serverName Name to set
     */
    private void addToRedis(String serverName) {
        try (Jedis jedis = jedisPool.getResource()) {
            jedis.set(Constants.REDIS_KEY_LOBBYSERVER, serverName);
        }
    }

    @Override
    protected void onServiceCreated(DockerObjTemplate.CreateResponse createResponse) {
        super.onServiceCreated(createResponse);

        this.addToRedis(this.getConfigObject().getName());
    }

}
