package de.derteufelqwe.bungeeplugin.events;

import de.derteufelqwe.bungeeplugin.redis.messages.RedisPlayerServerChange;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import net.md_5.bungee.api.Callback;
import net.md_5.bungee.api.event.AsyncEvent;

import javax.annotation.Nullable;

/**
 * Corresponds to {@link de.derteufelqwe.bungeeplugin.redis.messages.RedisPlayerServerChange}
 */
@Getter
@ToString
@EqualsAndHashCode
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

    public BungeePlayerServerChangeEvent(RedisPlayerServerChange redisEvent, Callback<BungeePlayerServerChangeEvent> done) {
        super(done);
        this.playerName = redisEvent.getUsername();
        this.oldServer = redisEvent.getOldServer();
        this.newServer = redisEvent.getNewServer();
    }


}
