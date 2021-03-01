package de.derteufelqwe.ServerManager.registry.deserializers;

import com.google.gson.*;
import de.derteufelqwe.ServerManager.registry.objects.RESTError;

import java.lang.reflect.Type;

public class RESTErrorDeserializer implements JsonDeserializer<RESTError> {

    @Override
    public RESTError deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
        JsonObject object = json.getAsJsonObject();
        RESTError error = new RESTError();

        error.setCode(object.get("code").getAsString());
        error.setMessage(object.get("message").getAsString());
        if (object.has("detail"))
            error.setDetails(object.get("detail").toString());

        return error;
    }
}
