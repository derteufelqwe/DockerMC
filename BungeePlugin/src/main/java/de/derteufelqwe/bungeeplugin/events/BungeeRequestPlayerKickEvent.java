package de.derteufelqwe.bungeeplugin.events;

import de.derteufelqwe.bungeeplugin.redis.messages.RedisRequestPlayerKick;
import de.derteufelqwe.bungeeplugin.redis.messages.RedisRequestPlayerServerSend;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import net.md_5.bungee.api.Callback;
import net.md_5.bungee.api.event.AsyncEvent;

/**
 * Requests that a user gets kicked
 * Corresponds to {@link de.derteufelqwe.bungeeplugin.redis.messages.RedisRequestPlayerKick}
 */
@Getter
@ToString
@EqualsAndHashCode
public class BungeeRequestPlayerKickEvent extends AsyncEvent<BungeeRequestPlayerKickEvent> {

    private String playerName;
    private String reason;


    public BungeeRequestPlayerKickEvent(String playerName, String reason, Callback<BungeeRequestPlayerKickEvent> done) {
        super(done);
        this.playerName = playerName;
        this.reason = reason;
    }

    public BungeeRequestPlayerKickEvent(RedisRequestPlayerKick redisEvent, Callback<BungeeRequestPlayerKickEvent> done) {
        super(done);
        this.playerName = redisEvent.getUsername();
        this.reason = redisEvent.getReason();
    }


}
