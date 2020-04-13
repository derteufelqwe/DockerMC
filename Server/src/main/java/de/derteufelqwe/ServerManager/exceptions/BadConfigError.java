package de.derteufelqwe.ServerManager.exceptions;

public class BadConfigError extends DockerMCException {

    public BadConfigError() {
        super();
    }

    public BadConfigError(String message, Object... args) {
        super(message, args);
    }

    public BadConfigError(String message, Throwable cause) {
        super(message, cause);
    }

    public BadConfigError(Throwable cause) {
        super(cause);
    }

    protected BadConfigError(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
