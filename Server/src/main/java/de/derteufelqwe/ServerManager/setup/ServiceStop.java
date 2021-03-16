package de.derteufelqwe.ServerManager.setup;

/**
 * Possible results for service destruction
 */
public enum ServiceStop {
    UNKNOWN,            // Not set
    OK,                 // Successful
    NOT_RUNNING,        // Service wasn't running
    NOT_CONFIGURED,     // Config missing
    FAILED_GENERIC      // Generic failure
}
