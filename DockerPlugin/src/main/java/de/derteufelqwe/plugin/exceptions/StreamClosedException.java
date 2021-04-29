package de.derteufelqwe.plugin.exceptions;

import de.derteufelqwe.commons.exceptions.DockerMCException;

/**
 * Thrown when a FIFO file gets deleted and thus the stream on it gets closed
 */
public class StreamClosedException extends DockerMCException {

    public StreamClosedException(String fileName) {
        super("%s FIFO file stream closed.", fileName);
    }
}
