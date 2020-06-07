package de.derteufelqwe.commons.config.providers;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.util.ArrayList;
import java.util.List;

public class DefaultGsonProvider implements GsonProvider {

    public DefaultGsonProvider() {

    }

    protected List<TypeAdapterContainer> getTypeAdapters() {
        return new ArrayList<>();
    }

    protected List<TypeAdapterContainer> getTypeHierarchyAdapters() {
        return new ArrayList<>();
    }

    protected GsonBuilder getBuilder() {
        GsonBuilder builder = new GsonBuilder();
        builder.setPrettyPrinting();
        builder.enableComplexMapKeySerialization(); // Complex types get serialized when used as Map keys instead of just using .toString()

        for (TypeAdapterContainer container : this.getTypeAdapters()) {
            builder.registerTypeAdapter(container.getType(), container.getTypeAdapter());
        }

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
