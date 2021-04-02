package de.derteufelqwe.nodewatcher.exceptions;

import de.derteufelqwe.commons.exceptions.DockerMCException;

/**
 * Thrown when reading hardware resource information from the docker host failed.
 */
public class InvalidHostResourcesException extends DockerMCException {

    public InvalidHostResourcesException(String message, Object... args) {
        super(message, args);
    }
}
