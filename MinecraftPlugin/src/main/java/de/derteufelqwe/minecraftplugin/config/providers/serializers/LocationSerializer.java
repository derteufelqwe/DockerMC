package de.derteufelqwe.minecraftplugin.config.providers.serializers;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import org.bukkit.Location;

import java.lang.reflect.Type;

public class LocationSerializer implements JsonSerializer<Location> {

    @Override
    public JsonElement serialize(Location src, Type typeOfSrc, JsonSerializationContext context) {
        JsonObject data = new JsonObject();

        data.add("world", context.serialize(src.getWorld()));
        data.addProperty("x", src.getX());
        data.addProperty("y", src.getY());
        data.addProperty("z", src.getZ());
        data.addProperty("yaw", src.getYaw());
        data.addProperty("pitch", src.getPitch());

        return data;
    }

}
