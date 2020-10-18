package de.derteufelqwe.commons.config.providers;

import de.derteufelqwe.commons.config.providers.deserializers.ItemStackDeserializer;
import de.derteufelqwe.commons.config.providers.deserializers.LocationDeserializer;
import de.derteufelqwe.commons.config.providers.deserializers.PlayerDeserializer;
import de.derteufelqwe.commons.config.providers.deserializers.WorldDeserializer;
import de.derteufelqwe.commons.config.providers.serializers.ItemStackSerializer;
import de.derteufelqwe.commons.config.providers.serializers.LocationSerializer;
import de.derteufelqwe.commons.config.providers.serializers.PlayerSerializer;
import de.derteufelqwe.commons.config.providers.serializers.WorldSerializer;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.List;

/**
 * Implementation of {@link GsonProvider}, which has support for Minecraft objects
 */
public class MinecraftGsonProvider extends DefaultGsonProvider {

    @Override
    protected List<TypeAdapterContainer> getTypeAdapters() {
        List<TypeAdapterContainer> list = super.getTypeAdapters();

        list.add(new TypeAdapterContainer(Location.class, new LocationSerializer()));
        list.add(new TypeAdapterContainer(Location.class, new LocationDeserializer()));
        list.add(new TypeAdapterContainer(ItemStack.class, new ItemStackSerializer()));
        list.add(new TypeAdapterContainer(ItemStack.class, new ItemStackDeserializer()));

        return list;
    }

    @Override
    protected List<TypeAdapterContainer> getTypeHierarchyAdapters() {
        List<TypeAdapterContainer> list = super.getTypeHierarchyAdapters();

        list.add(new TypeAdapterContainer(World.class, new WorldSerializer()));
        list.add(new TypeAdapterContainer(World.class, new WorldDeserializer()));
        list.add(new TypeAdapterContainer(Player.class, new PlayerSerializer()));
        list.add(new TypeAdapterContainer(Player.class, new PlayerDeserializer()));

        return list;
    }
    
}
