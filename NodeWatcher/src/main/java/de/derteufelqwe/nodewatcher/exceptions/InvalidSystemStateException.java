package de.derteufelqwe.nodewatcher.exceptions;


/**
 * Indicates that the system is in a state, that makes it unable to run correctly, so the program needs to be shut down.
 */
public class InvalidSystemStateException extends RuntimeException {

    public InvalidSystemStateException(String message) {
        super(message);
    }

    public InvalidSystemStateException(String message, Object... args) {
        super(String.format(message, args));
    }

}
