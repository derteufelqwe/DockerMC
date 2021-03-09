package de.derteufelqwe.commons.exceptions;

/**
 * Called when a value is not found in redis or the db when it should be there
 */
public class EntryNotFoundException extends BaseException {

    public EntryNotFoundException() {
        super();
    }

    public EntryNotFoundException(String message, Object... args) {
        super(message, args);
    }
}
