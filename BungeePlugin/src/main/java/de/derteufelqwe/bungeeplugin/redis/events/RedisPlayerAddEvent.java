package de.derteufelqwe.bungeeplugin.redis.events;

import com.google.gson.annotations.Expose;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class RedisPlayerAddEvent extends RedisEvent {

    @Expose
    private String username;

}
