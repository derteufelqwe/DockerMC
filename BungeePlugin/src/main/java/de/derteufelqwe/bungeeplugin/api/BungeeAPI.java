package de.derteufelqwe.bungeeplugin.api;

import de.derteufelqwe.bungeeplugin.BungeePlugin;
import de.derteufelqwe.bungeeplugin.events.BungeeRequestPlayerKickEvent;
import de.derteufelqwe.bungeeplugin.redis.PlayerData;
import de.derteufelqwe.bungeeplugin.redis.RedisDataManager;
import de.derteufelqwe.commons.exceptions.NotFoundException;
import de.derteufelqwe.commons.protobuf.RedisMessages;
import net.md_5.bungee.api.Callback;
import net.md_5.bungee.api.ProxyServer;

import java.util.UUID;

/**
 * API for general BungeeCord stuff
 */
public class BungeeAPI {

    private final RedisDataManager redisDataManager = BungeePlugin.getRedisDataManager();
    private RedisMessages.BungeeMessageBase messageBase;


    public BungeeAPI() {
        this.messageBase = RedisMessages.BungeeMessageBase.newBuilder()
                .setBungeeCordId(BungeePlugin.BUNGEECORD_ID)
                .build();
    }


    private void kickPlayer(UUID playerId, String username, String reason) {
        ProxyServer.getInstance().getPluginManager().callEvent(new BungeeRequestPlayerKickEvent(playerId, username, reason, new Callback<BungeeRequestPlayerKickEvent>() {
            @Override
            public void done(BungeeRequestPlayerKickEvent bungeeRequestPlayerKickEvent, Throwable throwable) {
                System.out.println("Kicked player " + playerId + ", " + username + ".");
            }
        }));

        RedisMessages.RequestPlayerKick requestPlayerKick = RedisMessages.RequestPlayerKick.newBuilder()
                .setBase(this.messageBase)
                .setUuid(RedisMessages.UUID.newBuilder()
                        .setData(playerId.toString())
                        .build())
                .setUsername(username)
                .setReason(reason)
                .build();

        redisDataManager.sendMessage(requestPlayerKick);
    }

    public void kickPlayer(UUID playerId, String reason) throws NotFoundException {
        PlayerData playerData = this.redisDataManager.getPlayer(playerId);

        if (playerData == null) {
            throw new NotFoundException("Player %s not online.", playerId);
        }

        this.kickPlayer(playerId, playerData.getUsername(), reason);
    }

    public void kickPlayer(String playerName, String reason) throws NotFoundException {
        PlayerData playerData = this.redisDataManager.getPlayer(playerName);

        if (playerData == null) {
            throw new NotFoundException("Player %s not online.", playerName);
        }

        try {
            UUID uuid = UUID.fromString(playerData.getUuid());
            this.kickPlayer(uuid, playerName, reason);

        } catch (IllegalArgumentException e) {
            System.err.println("Redis has invalid uuid (" + playerData.getUuid() + ") for player " + playerName);
        }

    }

}
