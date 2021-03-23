package de.derteufelqwe.nodewatcher.exceptions;

import de.derteufelqwe.commons.exceptions.DockerMCException;

/**
 * Raised by the {@link de.derteufelqwe.nodewatcher.stats.ContainerStatsCallback} when the callback should end
 */
public class ContainerNoLongerExistsException extends DockerMCException {

    public ContainerNoLongerExistsException(String id) {
        super("Container %s no longer available. Removing it.", id);
    }

}
