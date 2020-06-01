package de.derteufelqwe.commons.config.providers.serializers;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import org.bukkit.World;

import java.lang.reflect.Type;

public class WorldSerializer implements JsonSerializer<World> {

    @Override
    public JsonElement serialize(World src, Type typeOfSrc, JsonSerializationContext context) {
        JsonPrimitive worldJson = new JsonPrimitive(src.getUID().toString());

        return worldJson;
    }
}
