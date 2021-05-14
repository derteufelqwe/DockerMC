package de.derteufelqwe.ServerManager.setup;

/**
 * Possible results for service creation
 */
public enum ServiceStart {
    UNKNOWN,            // Not set
    OK,                 // Successful
    RUNNING,            // Was already running
    CREATED,            // Got newly created
    NOT_CONFIGURED,     // Config missing
    FAILED_GENERIC,      // Generic failure
    UPDATED,            // Updated
    NOT_REQUIRED,       // Update not required
    DESTROYED          // Got destroyed
}
