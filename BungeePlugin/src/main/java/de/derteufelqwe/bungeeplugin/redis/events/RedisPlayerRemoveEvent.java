package de.derteufelqwe.bungeeplugin.redis.events;

import com.google.gson.annotations.Expose;
import lombok.AllArgsConstructor;
import lombok.Data;

@AllArgsConstructor
@Data
public class RedisPlayerRemoveEvent extends RedisEvent {

    @Expose
    private String username;
    
}
