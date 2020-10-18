package de.derteufelqwe.commons.config;

import com.google.gson.ExclusionStrategy;
import com.google.gson.FieldAttributes;
import de.derteufelqwe.commons.config.annotations.Exclude;

import java.lang.annotation.Annotation;
import java.util.List;

/**
 * Used to exclude fields marked with @{@link de.derteufelqwe.commons.config.annotations.Exclude}
 */
public class DefaultExclusionStrategy implements ExclusionStrategy {

    @Override
    public boolean shouldSkipField(FieldAttributes f) {
        Annotation excludeAnnotation = f.getAnnotation(Exclude.class);

        if (excludeAnnotation != null) {
            return true;
        }

        return false;
    }

    @Override
    public boolean shouldSkipClass(Class<?> clazz) {
        return false;
    }
}
