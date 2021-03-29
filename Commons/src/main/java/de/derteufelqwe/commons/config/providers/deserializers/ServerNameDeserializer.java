package de.derteufelqwe.commons.config.providers.deserializers;

import com.google.gson.*;
import de.derteufelqwe.commons.docker.ServerName;

import java.lang.reflect.Type;

public class ServerNameDeserializer implements JsonDeserializer<ServerName> {

    @Override
    public ServerName deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
        JsonPrimitive obj = (JsonPrimitive) json;

        return new ServerName(obj.getAsString());
    }
}
