package de.derteufelqwe.bungeeplugin.redis.messages;

import com.google.gson.annotations.Expose;
import de.derteufelqwe.bungeeplugin.redis.MessageType;
import de.derteufelqwe.bungeeplugin.redis.RedisPubSubData;
import lombok.AllArgsConstructor;
import lombok.Data;

import javax.annotation.Nullable;
import java.util.UUID;

/**
 * Sent when a player should be kicked
 */
@Data
@AllArgsConstructor
public class RedisRequestPlayerKick extends RedisPubSubData {

    @Expose
    private String username;

    @Expose
    @Nullable
    private String reason;


    @Override
    public MessageType getMessageType() {
        return MessageType.REQUEST_PLAYER_KICK;
    }

}
