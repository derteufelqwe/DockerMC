package de.derteufelqwe.bungeeplugin.exceptions;

/**
 * Raised in the PlayerCache loader when the player is not present in the cache
 */
public class UserNotFoundException extends RuntimeException {

    public UserNotFoundException() {
        super();
    }

}
