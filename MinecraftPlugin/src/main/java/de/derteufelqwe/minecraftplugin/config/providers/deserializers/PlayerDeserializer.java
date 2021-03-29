package de.derteufelqwe.minecraftplugin.config.providers.deserializers;

import com.google.gson.*;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.lang.reflect.Type;
import java.util.UUID;

public class PlayerDeserializer implements JsonDeserializer<Player> {

    @Override
    public Player deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
        JsonObject data = (JsonObject) json;

        UUID playerId = UUID.fromString(data.get("uuid").getAsString());

        return Bukkit.getPlayer(playerId);
    }

}
