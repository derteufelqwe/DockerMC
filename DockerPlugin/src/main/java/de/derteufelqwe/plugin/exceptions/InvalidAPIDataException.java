package de.derteufelqwe.plugin.exceptions;

/**
 * Thrown when docker sends incomplete or completely missing data
 */
public class InvalidAPIDataException extends RuntimeException {

    public InvalidAPIDataException(String message) {
        super(message);
    }
}
