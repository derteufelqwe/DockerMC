package de.derteufelqwe.bungeeplugin.redis.events;

import com.google.gson.annotations.Expose;
import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * Called when a players joins the BungeeCord network
 */
@Data
@AllArgsConstructor
public class RedisPlayerAddEvent extends RedisEvent {

    @Expose
    private String username;

}
