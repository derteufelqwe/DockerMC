package de.derteufelqwe.bungeeplugin.events;

import de.derteufelqwe.bungeeplugin.redis.messages.RedisPlayerServerChange;
import de.derteufelqwe.bungeeplugin.redis.messages.RedisRequestPlayerServerSend;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import net.md_5.bungee.api.Callback;
import net.md_5.bungee.api.event.AsyncEvent;

import javax.annotation.Nullable;

/**
 * Requests that a user gets send to a new server
 * Corresponds to {@link de.derteufelqwe.bungeeplugin.redis.messages.RedisRequestPlayerServerSend}
 */
@Getter
@ToString
@EqualsAndHashCode
public class BungeeRequestPlayerServerSendEvent extends AsyncEvent<BungeeRequestPlayerServerSendEvent> {

    private String playerName;
    private String targetServer;


    public BungeeRequestPlayerServerSendEvent(String playerName, String targetServer, Callback<BungeeRequestPlayerServerSendEvent> done) {
        super(done);
        this.playerName = playerName;
        this.targetServer = targetServer;
    }

    public BungeeRequestPlayerServerSendEvent(RedisRequestPlayerServerSend redisEvent, Callback<BungeeRequestPlayerServerSendEvent> done) {
        super(done);
        this.playerName = redisEvent.getUsername();
        this.targetServer = redisEvent.getTargetServer();
    }


}
