package de.derteufelqwe.bungeeplugin.redis.messages;

import com.google.gson.annotations.Expose;
import de.derteufelqwe.bungeeplugin.redis.MessageType;
import de.derteufelqwe.bungeeplugin.redis.RedisPubSubData;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.UUID;


/**
 * Called when a player leaves the BungeeCord network
 */
@Data
@AllArgsConstructor
public class RedisPlayerLeaveNetwork extends RedisPubSubData {

    @Expose
    private UUID uuid;

    @Expose
    private String username;


    @Override
    public MessageType getMessageType() {
        return MessageType.PLAYER_LEAVE;
    }

}
