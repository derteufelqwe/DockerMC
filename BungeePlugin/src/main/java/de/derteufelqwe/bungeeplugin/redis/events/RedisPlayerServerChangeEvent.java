package de.derteufelqwe.bungeeplugin.redis.events;

import com.google.gson.annotations.Expose;
import lombok.AllArgsConstructor;
import lombok.Data;


/**
 * Called when a player changes the Server on the BungeeCord network
 */
@Data
@AllArgsConstructor
public class RedisPlayerServerChangeEvent extends RedisEvent {

    @Expose
    private String username;

}
