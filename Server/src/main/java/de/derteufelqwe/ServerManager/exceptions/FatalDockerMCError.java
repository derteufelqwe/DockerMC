package de.derteufelqwe.ServerManager.exceptions;

import de.derteufelqwe.commons.exceptions.DockerMCException;

/**
 * Thrown when the code can't handle certain events
 */
public class FatalDockerMCError extends DockerMCException {

    public FatalDockerMCError() {
        super();
    }

    public FatalDockerMCError(String message) {
        super(message);
    }

    public FatalDockerMCError(String message, Object... args) {
        super(message, args);
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
