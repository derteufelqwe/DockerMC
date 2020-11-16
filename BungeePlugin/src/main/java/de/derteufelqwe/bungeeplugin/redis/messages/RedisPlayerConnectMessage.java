package de.derteufelqwe.bungeeplugin.redis.messages;

import com.google.gson.annotations.Expose;
import de.derteufelqwe.bungeeplugin.redis.RedisPubSubData;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class RedisPlayerConnectMessage extends RedisPubSubData {

    @Expose
    private String username;

    @Expose
    private String targetBungee;

    @Expose
    private String targetServer;

}
