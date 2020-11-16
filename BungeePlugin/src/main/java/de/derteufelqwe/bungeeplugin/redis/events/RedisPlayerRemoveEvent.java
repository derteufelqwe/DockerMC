package de.derteufelqwe.bungeeplugin.redis.events;

import com.google.gson.annotations.Expose;
import de.derteufelqwe.bungeeplugin.redis.RedisPubSubData;
import lombok.AllArgsConstructor;
import lombok.Data;


/**
 * Called when a player leaves the BungeeCord network
 */
@Data
@AllArgsConstructor
public class RedisPlayerRemoveEvent extends RedisPubSubData {

    @Expose
    private String username;
    
}
