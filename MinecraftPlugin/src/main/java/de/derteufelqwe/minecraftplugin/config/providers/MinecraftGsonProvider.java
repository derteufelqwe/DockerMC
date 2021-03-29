package de.derteufelqwe.minecraftplugin.config.providers;

import de.derteufelqwe.commons.config.providers.DefaultGsonProvider;
import de.derteufelqwe.commons.config.providers.GsonProvider;
import de.derteufelqwe.commons.config.providers.TypeAdapterContainer;
import de.derteufelqwe.minecraftplugin.config.providers.deserializers.*;
import de.derteufelqwe.minecraftplugin.config.providers.serializers.*;
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
