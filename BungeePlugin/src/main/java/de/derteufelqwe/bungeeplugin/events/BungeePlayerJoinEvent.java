package de.derteufelqwe.bungeeplugin.events;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import net.md_5.bungee.api.Callback;
import net.md_5.bungee.api.event.AsyncEvent;

import java.util.UUID;

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

    private UUID playerId;

    private String playerName;

    public BungeePlayerJoinEvent(UUID playerId,String playerName, Callback<BungeePlayerJoinEvent> done) {
        super(done);
        this.playerId = playerId;
        this.playerName = playerName;
    }

}
