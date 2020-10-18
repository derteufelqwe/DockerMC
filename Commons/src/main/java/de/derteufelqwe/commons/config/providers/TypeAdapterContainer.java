package de.derteufelqwe.commons.config.providers;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * Container to house entries for {@link com.google.gson.Gson}s TypeAdapters.
 */
@Data
@AllArgsConstructor
public class TypeAdapterContainer {

    private Class type;
    private Object typeAdapter;

}
