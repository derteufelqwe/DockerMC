package de.derteufelqwe.ServerManager.exceptions;

public class InvalidStateError extends DockerMCException {

    public InvalidStateError() {
        super();
    }

    public InvalidStateError(String message, Object... args) {
        super(message, args);
    }

    public InvalidStateError(String message, Throwable cause) {
        super(message, cause);
    }

    public InvalidStateError(Throwable cause) {
        super(cause);
    }

    protected InvalidStateError(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
