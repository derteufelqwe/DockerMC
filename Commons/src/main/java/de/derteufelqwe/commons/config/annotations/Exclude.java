package de.derteufelqwe.commons.config.annotations;

import java.lang.annotation.*;

/**
 * Used to exclude Fields from serialization
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface Exclude {
}
