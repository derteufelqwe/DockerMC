package de.derteufelqwe.bungeeplugin.exceptions;

/**
 * Indicates that a config is faulty
 */
public class ConfigException extends BaseException {

    public ConfigException() {
        super();
    }

    public ConfigException(String message, Object... args) {
        super(message, args);
    }

    public ConfigException(String message, Throwable cause) {
        super(message, cause);
    }

    public ConfigException(Throwable cause) {
        super(cause);
    }

    protected ConfigException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }

}
