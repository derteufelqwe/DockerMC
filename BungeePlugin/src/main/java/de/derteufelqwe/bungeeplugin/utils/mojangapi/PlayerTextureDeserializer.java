package de.derteufelqwe.bungeeplugin.utils.mojangapi;

import com.google.gson.*;

import java.lang.reflect.Type;
import java.sql.Timestamp;
import java.util.Base64;
import java.util.Map;
import java.util.UUID;

/**
 * Custom deserializer for Mojangs Player texture
 */
public class PlayerTextureDeserializer implements JsonDeserializer<MojangAPIProfile.PlayerTexture> {

    @Override
    public MojangAPIProfile.PlayerTexture deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
        if (json == null) {
            return null;
        }

        String newJsonData = new String(Base64.getDecoder().decode(json.getAsString()));
        Map<String, Object> newData = (Map<String, Object>) new Gson().fromJson(newJsonData, Map.class);

        String rawUuidString = (String) newData.get("profileId");
        String uuidString  = rawUuidString.substring(0, 8) + "-" + rawUuidString.substring(8, 12) + "-" +
                rawUuidString.substring(12, 16) + "-" + rawUuidString.substring(16, 20) + "-" + rawUuidString.substring(20, 32);

        MojangAPIProfile.PlayerTexture texture = new MojangAPIProfile.PlayerTexture(
                new Timestamp(Math.round((Double) newData.get("timestamp"))),
                UUID.fromString(uuidString),
                (String) newData.get("profileName"),
                null,
                null
        );

        try {
            for (Map.Entry<String, Map<String, String>> entry : ((Map<String, Map<String, String>>) newData.get("textures")).entrySet()) {
                if (entry.getKey().equals("SKIN")) {
                    texture.setSkinUrl(entry.getValue().get("url"));

                } else if (entry.getKey().equals("CAPE")) {
                    texture.setSkinUrl(entry.getValue().get("url"));
                }
            }

        } catch (Exception e) {
            System.err.println("Failed to parse players texture.");
            e.printStackTrace();
        }

        return texture;
    }

}
