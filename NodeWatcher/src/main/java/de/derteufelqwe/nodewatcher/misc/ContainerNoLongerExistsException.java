package de.derteufelqwe.nodewatcher.misc;

/**
 * Raised by the {@link de.derteufelqwe.nodewatcher.stats.ContainerStatsCallback} when the callback should end
 */
public class ContainerNoLongerExistsException extends RuntimeException {

    public ContainerNoLongerExistsException(String message) {
        super(message);
    }

    public ContainerNoLongerExistsException(String message, Object... args) {
        super(String.format(message, args));
    }

}
