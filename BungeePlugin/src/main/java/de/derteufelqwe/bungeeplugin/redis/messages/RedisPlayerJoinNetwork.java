package de.derteufelqwe.bungeeplugin.redis.messages;

import com.google.gson.annotations.Expose;
import de.derteufelqwe.bungeeplugin.redis.MessageType;
import de.derteufelqwe.bungeeplugin.redis.RedisPubSubData;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.UUID;

/**
 * Sent through redis when a player a player joins the network.
 * The message only gets processed on nodes, that didn't send it
 */
@Data
@AllArgsConstructor
public class RedisPlayerJoinNetwork extends RedisPubSubData {

    @Expose
    private UUID uuid;

    @Expose
    private String username;

    @Override
    public MessageType getMessageType() {
        return MessageType.PLAYER_JOIN;
    }

}
