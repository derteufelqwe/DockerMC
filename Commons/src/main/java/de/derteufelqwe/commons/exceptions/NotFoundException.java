package de.derteufelqwe.commons.exceptions;

/**
 * Called when a value is not found in redis or the db when it should be there
 */
public class NotFoundException extends BaseException {

    public NotFoundException() {
        super();
    }

    public NotFoundException(String message, Object... args) {
        super(message, args);
    }
}
