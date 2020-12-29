package de.derteufelqwe.bungeeplugin.utils.mojangapi;

import com.google.gson.*;

import java.lang.reflect.Type;

/**
 * Custom deserializer for Mojangs Profile API
 */
public class MojangAPIProfileDeserializer implements JsonDeserializer<MojangAPIProfile> {

    @Override
    public MojangAPIProfile deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
        JsonObject jsonObj = json.getAsJsonObject();
        JsonArray properties = jsonObj.getAsJsonArray("properties");

        JsonElement texture = null;

        for (JsonElement prop : properties) {
            if (prop.getAsJsonObject().get("name").getAsString().equals("textures")) {
                texture = prop.getAsJsonObject().get("value");
            }

        }

        return new MojangAPIProfile(
                jsonObj.get("id").getAsString(),
                jsonObj.get("name").getAsString(),
                jsonObj.get("legacy").getAsBoolean(),
                context.deserialize(texture, MojangAPIProfile.PlayerTexture.class)
        );
    }
}
