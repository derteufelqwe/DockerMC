package de.derteufelqwe.commons.exceptions;

/**
 * Thrown when a docker object doesn't have an expected value mostly due to NullPointerExceptions
 */
public class DockerAPIIncompleteException extends DockerMCException {

    public DockerAPIIncompleteException(Throwable cause, String message, Object... args) {
        super(cause, message, args);
    }
}
