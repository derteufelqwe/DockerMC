package de.derteufelqwe.bungeeplugin.redis.messages;

import com.google.gson.annotations.Expose;
import de.derteufelqwe.bungeeplugin.redis.MessageType;
import de.derteufelqwe.bungeeplugin.redis.RedisPubSubData;
import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * Sent when a player needs to be sent to another server
 */
@Data
@AllArgsConstructor
public class RedisRequestPlayerServerSend extends RedisPubSubData {

    @Expose
    private String username;

    /**
     * BungeeCord server on which the user is currently on
     */
    @Expose
    private String targetBungee;

    /**
     * The server where the user should be connected to
     */
    @Expose
    private String targetServer;


    @Override
    public MessageType getMessageType() {
        return MessageType.REQUEST_PLAYER_SERVER_CHANGE;
    }

}
