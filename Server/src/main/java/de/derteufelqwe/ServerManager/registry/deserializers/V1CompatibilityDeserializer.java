package de.derteufelqwe.ServerManager.registry.deserializers;

import com.google.gson.*;
import de.derteufelqwe.ServerManager.registry.objects.RESTError;
import de.derteufelqwe.ServerManager.registry.objects.V1Compatibility;

import javax.xml.crypto.Data;
import java.lang.reflect.Type;
import java.util.Date;
import java.util.Map;

public class V1CompatibilityDeserializer implements JsonDeserializer<V1Compatibility> {

    @Override
    public V1Compatibility deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
        JsonObject object = json.getAsJsonObject();
        V1Compatibility compatibility = new V1Compatibility();

        // Guaranteed available entries
        String created = object.get("created").getAsString().substring(0, 19);
        compatibility.setId(object.get("id").getAsString());
        compatibility.setCreated(context.deserialize(new JsonPrimitive(created), Date.class));
        compatibility.setContainerConfig(context.deserialize(object.get("container_config"), Map.class));

        object.remove("id");
        object.remove("created");
        object.remove("container_config");

        // Mostly available objects
        if (object.has("parent")) {
            compatibility.setParent(object.get("parent").getAsString());
            object.remove("parent");
        }

        // Rest
        compatibility.setOther(context.deserialize(object, Map.class));

        return compatibility;
    }
}
