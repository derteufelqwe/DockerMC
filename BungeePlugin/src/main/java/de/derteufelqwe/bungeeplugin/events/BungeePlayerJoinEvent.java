package de.derteufelqwe.bungeeplugin.events;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import net.md_5.bungee.api.Callback;
import net.md_5.bungee.api.event.AsyncEvent;

/**
 * Fired when a player joins the Network.
 * Corresponds to {@link de.derteufelqwe.bungeeplugin.redis.messages.RedisPlayerJoinNetwork}
 *
 * When this event gets fired, you can be sure, that all required objects in the DB and redis are set
 */
@Getter
@ToString
@EqualsAndHashCode
public class BungeePlayerJoinEvent extends AsyncEvent<BungeePlayerJoinEvent> {

    private String playerName;

    public BungeePlayerJoinEvent(String playerName, Callback<BungeePlayerJoinEvent> done) {
        super(done);
        this.playerName = playerName;
    }

}
