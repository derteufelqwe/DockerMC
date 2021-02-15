package de.derteufelqwe.bungeeplugin.events;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import net.md_5.bungee.api.Callback;
import net.md_5.bungee.api.event.AsyncEvent;

import javax.annotation.Nullable;
import java.util.UUID;

/**
 * Corresponds to {@link de.derteufelqwe.commons.protobuf.RedisMessages.PlayerChangeServer}
 */
@Getter
@ToString
@EqualsAndHashCode
public class BungeePlayerServerChangeEvent extends AsyncEvent<BungeePlayerServerChangeEvent> {

    private UUID playerId;
    private String playerName;
    @Nullable
    private String oldServer;
    private String newServer;


    public BungeePlayerServerChangeEvent(UUID playerId, String playerName, @Nullable String oldServer, String newServer, Callback<BungeePlayerServerChangeEvent> done) {
        super(done);
        this.playerId = playerId;
        this.playerName = playerName;
        this.oldServer = oldServer;
        this.newServer = newServer;
    }


}
