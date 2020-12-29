package de.derteufelqwe.bungeeplugin.events;

import net.md_5.bungee.api.Callback;
import net.md_5.bungee.api.event.AsyncEvent;

import javax.annotation.Nullable;

/**
 * Corresponds to {@link de.derteufelqwe.bungeeplugin.redis.messages.RedisPlayerServerChange}
 */
public class BungeePlayerServerChangeEvent extends AsyncEvent<BungeePlayerServerChangeEvent> {

    private String playerName;
    @Nullable private String oldServer;
    private String newServer;


    public BungeePlayerServerChangeEvent(String playerName, @Nullable String oldServer, String newServer, Callback<BungeePlayerServerChangeEvent> done) {
        super(done);
        this.playerName = playerName;
        this.oldServer = oldServer;
        this.newServer = newServer;
    }


}
