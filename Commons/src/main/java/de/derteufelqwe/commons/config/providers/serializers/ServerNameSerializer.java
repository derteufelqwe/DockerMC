package de.derteufelqwe.commons.config.providers.serializers;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import de.derteufelqwe.commons.docker.ServerName;
import org.bukkit.World;

import java.lang.reflect.Type;

public class ServerNameSerializer implements JsonSerializer<ServerName> {

    @Override
    public JsonElement serialize(ServerName src, Type typeOfSrc, JsonSerializationContext context) {
        JsonPrimitive serverJson = new JsonPrimitive(src.fullName());

        return serverJson;
    }
}
