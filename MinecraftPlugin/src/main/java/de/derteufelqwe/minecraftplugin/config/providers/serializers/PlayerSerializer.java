package de.derteufelqwe.minecraftplugin.config.providers.serializers;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import org.bukkit.entity.Player;

import java.lang.reflect.Type;

public class PlayerSerializer implements JsonSerializer<Player> {

    @Override
    public JsonElement serialize(Player src, Type typeOfSrc, JsonSerializationContext context) {
        JsonObject data = new JsonObject();

        data.addProperty("name", src.getName());
        data.addProperty("uuid", src.getUniqueId().toString());

        return data;
    }

}
