package de.derteufelqwe.commons.config.providers.deserializers;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import org.bukkit.inventory.ItemStack;

import java.lang.reflect.Type;
import java.util.Map;

public class ItemStackDeserializer implements JsonDeserializer<ItemStack> {

    @Override
    public ItemStack deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
        return ItemStack.deserialize(context.deserialize(json, Map.class));
    }

}
