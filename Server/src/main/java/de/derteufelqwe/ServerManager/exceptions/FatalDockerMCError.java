package de.derteufelqwe.ServerManager.exceptions;

/**
 * Thrown when the code can't handle certain events
 */
public class FatalDockerMCError extends RuntimeException {

    public FatalDockerMCError() {
        super();
    }

    public FatalDockerMCError(String message) {
        super(message);
    }

    public FatalDockerMCError(String message, Throwable cause) {
        super(message, cause);
    }

    public FatalDockerMCError(Throwable cause) {
        super(cause);
    }

    protected FatalDockerMCError(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
