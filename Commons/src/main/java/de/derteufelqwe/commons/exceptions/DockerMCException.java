package de.derteufelqwe.commons.exceptions;

public class DockerMCException extends RuntimeException {

    public DockerMCException() {
        super();
    }

    public DockerMCException(String message, Object... args) {
        super(String.format(message, args));
    }

    public DockerMCException(String message, Throwable cause) {
        super(message, cause);
    }

    public DockerMCException(Throwable cause) {
        super(cause);
    }

    public DockerMCException(Throwable cause, String message, Object... args) {
        super(String.format(message, args), cause);
    }

    protected DockerMCException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
