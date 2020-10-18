package de.derteufelqwe.commons.config.providers;

import com.google.gson.Gson;

/**
 * Interface to provide a method to create a {@link Gson} instance,
 * which is used to serialize configs to {@link java.util.Map}s
 */
public interface GsonProvider {

    Gson getGson();

}
