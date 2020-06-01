package de.derteufelqwe.commons.config.providers.deserializers;

import com.google.gson.*;
import org.bukkit.Bukkit;
import org.bukkit.World;

import java.lang.reflect.Type;
import java.util.UUID;

public class WorldDeserializer implements JsonDeserializer<World> {

    @Override
    public World deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
        JsonPrimitive obj = (JsonPrimitive) json;

        return Bukkit.getWorld(UUID.fromString(obj.getAsString()));
    }
}
