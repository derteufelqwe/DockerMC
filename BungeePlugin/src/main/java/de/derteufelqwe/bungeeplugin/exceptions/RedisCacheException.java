package de.derteufelqwe.bungeeplugin.exceptions;

import de.derteufelqwe.commons.exceptions.BaseException;

/**
 * Raised when the redis cache has a problem
 */
public class RedisCacheException extends BaseException {

    public RedisCacheException(String message, Object... args) {
        super(message, args);
    }

}
