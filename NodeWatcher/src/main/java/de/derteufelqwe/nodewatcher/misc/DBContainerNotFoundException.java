package de.derteufelqwe.nodewatcher.misc;

import de.derteufelqwe.commons.exceptions.DockerMCException;

/**
 * Raised when a container is not found in the database but should be
 */
public class DBContainerNotFoundException extends DockerMCException {

    public DBContainerNotFoundException(String id) {
        super("DBContainer %s not found.", id);
    }
}
