package de.derteufelqwe.bungeeplugin.exceptions;

import de.derteufelqwe.commons.exceptions.DockerMCException;

/**
 * Thrown when the api has errors
 */
public class DmcAPIException extends DockerMCException {

    public DmcAPIException(String message, Object... args) {
        super(message, args);
    }
}
