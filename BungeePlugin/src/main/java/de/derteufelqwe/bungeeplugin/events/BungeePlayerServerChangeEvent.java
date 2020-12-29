package de.derteufelqwe.bungeeplugin.events;

import net.md_5.bungee.api.Callback;
import net.md_5.bungee.api.event.AsyncEvent;

/**
 * Corresponds to {@link de.derteufelqwe.bungeeplugin.redis.messages.RedisPlayerServerChange}
 */
public class BungeePlayerServerChangeEvent extends AsyncEvent<BungeePlayerServerChangeEvent> {

    private String playerName;
    private String oldServer;
    private String newServer;

    public BungeePlayerServerChangeEvent(String playerName, String oldServer, String newServer, Callback<BungeePlayerServerChangeEvent> done) {
        super(done);
        this.playerName = playerName;
        this.oldServer = oldServer;
        this.newServer = newServer;
    }

}
