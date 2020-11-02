package de.derteufelqwe.ServerManager.exceptions;

/**
 * Used when an invalid config is loaded
 */
public class InvalidConfigException extends DockerMCException {

    public InvalidConfigException(String message, Object... args) {
        super(message, args);
    }

}
