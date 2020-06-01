package de.derteufelqwe.commons.config.providers.deserializers;

import com.google.gson.*;
import org.bukkit.Location;
import org.bukkit.World;

import java.lang.reflect.Type;

public class LocationDeserializer implements JsonDeserializer<Location> {

    @Override
    public Location deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
        JsonObject data = (JsonObject) json;

        return new Location(
                context.deserialize(data.get("world"), World.class),
                data.get("x").getAsInt(),
                data.get("y").getAsInt(),
                data.get("z").getAsInt(),
                data.get("yaw").getAsFloat(),
                data.get("pitch").getAsFloat()
        );
    }

}
