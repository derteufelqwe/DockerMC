package de.derteufelqwe.bungeeplugin.exceptions;

/**
 * Base for all exceptions
 */
public class BaseException extends RuntimeException {

    public BaseException() {
        super();
    }

    public BaseException(String message, Object... args) {
        super(String.format(message, args));
    }

    public BaseException(String message, Throwable cause) {
        super(message, cause);
    }

    public BaseException(Throwable cause) {
        super(cause);
    }

    protected BaseException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
