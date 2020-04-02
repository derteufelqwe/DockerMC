package de.derteufelqwe.ServerManager.exceptions;

public class InvalidServiceConfig extends DockerMCException {

    public InvalidServiceConfig() {
        super();
    }

    public InvalidServiceConfig(String message, Object... args) {
        super(message, args);
    }

    public InvalidServiceConfig(String message, Throwable cause) {
        super(message, cause);
    }

    public InvalidServiceConfig(Throwable cause) {
        super(cause);
    }

    protected InvalidServiceConfig(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
