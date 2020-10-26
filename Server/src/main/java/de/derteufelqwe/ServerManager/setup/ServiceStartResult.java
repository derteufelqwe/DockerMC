package de.derteufelqwe.ServerManager.setup;

/**
 * Possible results for service creation
 */
public enum ServiceStartResult {
    UNKOWN,             // Not set
    OK,                 // Successful
    RUNNING,            // Was already running
    NOT_CONFIGURED,     // Config missing
    FAILED_GENERIC      // Generic failure

}
