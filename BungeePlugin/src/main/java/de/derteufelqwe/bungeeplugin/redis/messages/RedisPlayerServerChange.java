package de.derteufelqwe.bungeeplugin.redis.messages;

import com.google.gson.annotations.Expose;
import de.derteufelqwe.bungeeplugin.redis.MessageType;
import de.derteufelqwe.bungeeplugin.redis.RedisPubSubData;
import lombok.AllArgsConstructor;
import lombok.Data;


/**
 * Called when a player changes the Server on the BungeeCord network
 */
@Data
@AllArgsConstructor
public class RedisPlayerServerChange extends RedisPubSubData {

    @Expose
    private String username;

    @Expose
    private String oldServer;

    @Expose
    private String newServer;


    public RedisPlayerServerChange(String username, String newServer) {
        this.username = username;
        this.newServer = newServer;
    }


    @Override
    public MessageType getMessageType() {
        return MessageType.PLAYER_SERVER_CHANGE;
    }

}
