package de.derteufelqwe.ServerManager.exceptions;

import de.derteufelqwe.commons.exceptions.DockerMCException;

/**
 * Used when an invalid config is loaded
 */
public class InvalidConfigException extends DockerMCException {

    public InvalidConfigException(String message, Object... args) {
        super(message, args);
    }

}
