package de.derteufelqwe.commons.config.providers;

import de.derteufelqwe.commons.config.providers.serializers.LocationSerializer;
import de.derteufelqwe.commons.config.providers.serializers.WorldSerializer;
import shaded.mcp.com.fasterxml.jackson.databind.Module;
import shaded.mcp.com.fasterxml.jackson.databind.module.SimpleModule;
import org.bukkit.Location;

import java.util.List;

/**
 * Enhanced version of the DefaultYamlProvider, which can also serialize Minecraft objects
 */
public class MinecraftYamlProvider extends DefaultYamlProvider {

    @Override
    protected List<Module> getModules() {
        List<Module> modules = super.getModules();

        SimpleModule module = new SimpleModule("MinecraftModule");
        module.addSerializer(new WorldSerializer());
        module.addSerializer(Location.class, new LocationSerializer());

        modules.add(module);
        return modules;
    }
}
