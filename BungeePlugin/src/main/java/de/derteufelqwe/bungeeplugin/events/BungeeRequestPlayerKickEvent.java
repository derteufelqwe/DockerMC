package de.derteufelqwe.bungeeplugin.events;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import net.md_5.bungee.api.Callback;
import net.md_5.bungee.api.event.AsyncEvent;

import java.util.UUID;

/**
 * Requests that a user gets kicked
 * Corresponds to {@link de.derteufelqwe.commons.protobuf.RedisMessages.RequestPlayerKick}
 */
@Getter
@ToString
@EqualsAndHashCode
public class BungeeRequestPlayerKickEvent extends AsyncEvent<BungeeRequestPlayerKickEvent> {

    private UUID playerId;
    private String playerName;
    private String reason;


    public BungeeRequestPlayerKickEvent(UUID playerId, String playerName, String reason, Callback<BungeeRequestPlayerKickEvent> done) {
        super(done);
        this.playerId = playerId;
        this.playerName = playerName;
        this.reason = reason;
    }


}
