package de.derteufelqwe.ServerManager.setup;

/**
 * Possible results for service updating
 */
public enum ServiceUpdate {
    UNKNOWN,            // Not set
    EXISTING,           // No updated happened
    DESTROYED,          // Got destroyed
    CREATED,            // Got newly created
    UPDATED,            // Updated
    NOT_REQUIRED,       // Update not required
    NOT_CONFIGURED,     // Not configured
    FAILED_GENERIC      // Generic failure
}
