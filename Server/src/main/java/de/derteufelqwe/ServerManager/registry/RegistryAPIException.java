package de.derteufelqwe.ServerManager.registry;

public class RegistryAPIException extends RuntimeException {

    public RegistryAPIException(String message, Object... args) {
        super(String.format(message, args));
    }

    public RegistryAPIException(String message, Throwable cause, Object... args) {
        super(String.format(message, args), cause);
    }
}
