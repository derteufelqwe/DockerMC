package de.derteufelqwe.commons.config.providers;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.lang.reflect.Type;

/**
 * Container to house entries for gsons typeAdapters.
 */
@Data
@AllArgsConstructor
public class TypeAdapterContainer {

    private Class type;
    private Object typeAdapter;

}
