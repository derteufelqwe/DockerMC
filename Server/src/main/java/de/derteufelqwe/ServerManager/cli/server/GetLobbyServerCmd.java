package de.derteufelqwe.ServerManager.cli.server;

import de.derteufelqwe.ServerManager.ServerManager;
import de.derteufelqwe.commons.Constants;
import lombok.extern.log4j.Log4j2;
import picocli.CommandLine;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.exceptions.JedisConnectionException;

@CommandLine.Command(name = "getLobby", description = "Shows the name of the lobby server as stored in redis")
@Log4j2
public class GetLobbyServerCmd implements Runnable {

    private JedisPool jedisPool = ServerManager.getRedisPool().getJedisPool();

    @Override
    public void run() {
        String lobbyServer = getLobbyServerNameFromRedis();

        if (lobbyServer == null || lobbyServer.equals("")) {
            log.warn("LobbyServer name not configured.");

        } else {
            log.info("LobbyServer name: '{}'.", lobbyServer);
        }
    }

    private String getLobbyServerNameFromRedis() {
        try (Jedis jedis = jedisPool.getResource()) {
            return jedis.get(Constants.REDIS_KEY_LOBBYSERVER);

        } catch (JedisConnectionException e) {
            log.warn("Failed to connect to redis server. Is it started?");
            return "";
        }
    }

}
