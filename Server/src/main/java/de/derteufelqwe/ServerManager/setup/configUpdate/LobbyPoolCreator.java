package de.derteufelqwe.ServerManager.setup.configUpdate;

import de.derteufelqwe.ServerManager.Docker;
import de.derteufelqwe.ServerManager.setup.servers.ServerPool;
import de.derteufelqwe.ServerManager.setup.templates.DockerObjTemplate;
import de.derteufelqwe.commons.Constants;
import org.springframework.data.redis.core.StringRedisTemplate;

class LobbyPoolCreator extends DMCServiceCreator<ServerPool> {

    protected StringRedisTemplate redisTemplate;

    public LobbyPoolCreator(Docker docker, StringRedisTemplate redisTemplate) {
        super(docker);
        this.redisTemplate = redisTemplate;
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
        this.redisTemplate.opsForValue().set(Constants.REDIS_KEY_LOBBYSERVER, serverName);
    }

    @Override
    protected void onServiceCreated(DockerObjTemplate.CreateResponse createResponse) {
        super.onServiceCreated(createResponse);

        this.addToRedis(this.getConfigObject().getName());
    }

}
