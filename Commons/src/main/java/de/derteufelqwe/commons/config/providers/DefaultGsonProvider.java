package de.derteufelqwe.commons.config.providers;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.TypeAdapter;
import com.google.gson.TypeAdapterFactory;
import com.google.gson.reflect.TypeToken;

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
