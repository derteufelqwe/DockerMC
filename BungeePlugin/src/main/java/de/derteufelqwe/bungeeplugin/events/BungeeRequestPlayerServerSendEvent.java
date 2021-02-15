package de.derteufelqwe.bungeeplugin.events;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import net.md_5.bungee.api.Callback;
import net.md_5.bungee.api.event.AsyncEvent;

import java.util.UUID;

/**
 * Requests that a user gets send to a new server
 * Corresponds to {@link de.derteufelqwe.commons.protobuf.RedisMessages.RequestPlayerSend}
 */
@Getter
@ToString
@EqualsAndHashCode
public class BungeeRequestPlayerServerSendEvent extends AsyncEvent<BungeeRequestPlayerServerSendEvent> {

    private UUID playerId;
    private String playerName;
    private String targetServer;


    public BungeeRequestPlayerServerSendEvent(UUID playerId, String playerName, String targetServer, Callback<BungeeRequestPlayerServerSendEvent> done) {
        super(done);
        this.playerId = playerId;
        this.playerName = playerName;
        this.targetServer = targetServer;
    }

}
