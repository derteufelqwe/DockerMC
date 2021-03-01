package de.derteufelqwe.ServerManager.registry.deserializers;

import com.google.gson.*;
import de.derteufelqwe.ServerManager.registry.objects.History;
import de.derteufelqwe.ServerManager.registry.objects.V1Compatibility;

import java.lang.reflect.Type;
import java.util.Map;

public class HistoryDeserializer implements JsonDeserializer<History> {

    private static final Gson gson = new Gson();

    @Override
    public History deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
        JsonObject object = json.getAsJsonObject();
        JsonPrimitive data = object.getAsJsonPrimitive("v1Compatibility");

        // Little workaround to be able to deserialize a field, that contains a json string (not a json object)
        JsonObject cleanedObject = gson.fromJson(data.getAsString(), JsonObject.class);

        return new History(context.deserialize(cleanedObject, V1Compatibility.class));
    }
}
