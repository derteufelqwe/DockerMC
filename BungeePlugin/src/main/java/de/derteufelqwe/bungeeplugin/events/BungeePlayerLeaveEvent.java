package de.derteufelqwe.bungeeplugin.events;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import net.md_5.bungee.api.Callback;
import net.md_5.bungee.api.event.AsyncEvent;

import java.util.UUID;

/**
 * Corresponds to {@link de.derteufelqwe.bungeeplugin.redis.messages.RedisPlayerLeaveNetwork}
 */
@Getter
@ToString
@EqualsAndHashCode
public class BungeePlayerLeaveEvent extends AsyncEvent<BungeePlayerLeaveEvent> {

    private UUID playerId;

    private String playerName;

    public BungeePlayerLeaveEvent(UUID playerId, String playerName, Callback<BungeePlayerLeaveEvent> done) {
        super(done);
        this.playerId = playerId;
        this.playerName = playerName;
    }

}
