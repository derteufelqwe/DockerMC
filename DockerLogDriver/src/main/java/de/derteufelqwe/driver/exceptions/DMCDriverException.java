package de.derteufelqwe.driver.exceptions;

/**
 * Custom exceptions
 */
public class DMCDriverException extends RuntimeException {

    public DMCDriverException(String message) {
        super(message);
    }

    public DMCDriverException(String message, Throwable cause) {
        super(message, cause);
    }
}
