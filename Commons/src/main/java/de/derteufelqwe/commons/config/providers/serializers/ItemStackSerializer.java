package de.derteufelqwe.commons.config.providers.serializers;

import com.google.gson.JsonElement;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import org.bukkit.inventory.ItemStack;

import java.lang.reflect.Type;

public class ItemStackSerializer implements JsonSerializer<ItemStack> {

    @Override
    public JsonElement serialize(ItemStack src, Type typeOfSrc, JsonSerializationContext context) {
        return context.serialize(src.serialize());
    }

}
