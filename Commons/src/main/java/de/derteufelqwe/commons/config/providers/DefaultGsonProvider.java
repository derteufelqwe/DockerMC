package de.derteufelqwe.commons.config.providers;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import de.derteufelqwe.commons.config.DefaultExclusionStrategy;
import de.derteufelqwe.commons.config.providers.deserializers.ServerNameDeserializer;
import de.derteufelqwe.commons.config.providers.serializers.ServerNameSerializer;
import de.derteufelqwe.commons.docker.ServerName;

import java.util.ArrayList;
import java.util.List;

/**
 * Default implementation of {@link GsonProvider}, which has no special serializers.
 * To add custom serializers, a class must inherit and overwrite {getTypeAdapters()} and {getTypeHierarchyAdapters()}.
 */
public class DefaultGsonProvider implements GsonProvider {

    public DefaultGsonProvider() {

    }

    protected List<TypeAdapterContainer> getTypeAdapters() {
        List<TypeAdapterContainer> adapters = new ArrayList<>();

        adapters.add(new TypeAdapterContainer(ServerName.class, new ServerNameSerializer()));
        adapters.add(new TypeAdapterContainer(ServerName.class, new ServerNameDeserializer()));

        return adapters;
    }

    protected List<TypeAdapterContainer> getTypeHierarchyAdapters() {
        return new ArrayList<>();
    }

    protected GsonBuilder getBuilder() {
        GsonBuilder builder = new GsonBuilder();
        builder.setPrettyPrinting();
        builder.enableComplexMapKeySerialization(); // Complex types get serialized when used as Map keys instead of just using .toString()
        builder.serializeNulls();
        builder.setExclusionStrategies(new DefaultExclusionStrategy());

        // Register TypeAdapters
        for (TypeAdapterContainer container : this.getTypeAdapters()) {
            builder.registerTypeAdapter(container.getType(), container.getTypeAdapter());
        }

        // Register TypeHierarchyAdapters
        for (TypeAdapterContainer container : this.getTypeHierarchyAdapters()) {
            builder.registerTypeHierarchyAdapter(container.getType(), container.getTypeAdapter());
        }

        return builder;
    }

    @Override
    public Gson getGson() {
        return getBuilder().create();
    }

}
