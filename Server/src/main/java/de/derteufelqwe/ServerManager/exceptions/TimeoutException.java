package de.derteufelqwe.ServerManager.exceptions;

import de.derteufelqwe.ServerManager.exceptions.DockerMCException;

public class TimeoutException extends DockerMCException {

    public TimeoutException() {
        super();
    }

    public TimeoutException(String message, Object... args) {
        super(message, args);
    }

    public TimeoutException(String message, Throwable cause) {
        super(message, cause);
    }

    public TimeoutException(Throwable cause) {
        super(cause);
    }

    protected TimeoutException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
