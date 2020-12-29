package de.derteufelqwe.bungeeplugin.events;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import net.md_5.bungee.api.Callback;
import net.md_5.bungee.api.event.AsyncEvent;

/**
 * Corresponds to {@link de.derteufelqwe.bungeeplugin.redis.messages.RedisPlayerLeaveNetwork}
 */
@Getter
@ToString
@EqualsAndHashCode
public class BungeePlayerLeaveEvent extends AsyncEvent<BungeePlayerLeaveEvent> {

    private String playerName;

    public BungeePlayerLeaveEvent(String playerName, Callback<BungeePlayerLeaveEvent> done) {
        super(done);
        this.playerName = playerName;
    }

}
