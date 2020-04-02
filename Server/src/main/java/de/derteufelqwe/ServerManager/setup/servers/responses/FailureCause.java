package de.derteufelqwe.ServerManager.setup.servers.responses;

public enum  FailureCause {
    DEFAULT,                // Default
    NOT_FAILED,             // Not actually failed
    UNKNOWN,                // Unknown
    CONTAINER_FAIL,         // Error in container
    TASKS_NOT_SPAWNED ,     // A service couldn't spawn all its tasks
    NOT_ENOUGH_RAM,         // Host has not enough RAM
    NO_NODE,                // No suitable node found
    SERVICE_NOT_STARTED,    // Service failed to start
    SERVICE_ZERO_TASKS,     // A Service is configured with 0 Tasks
    CONTAINER_STARTUP_FAIL  // Conainer(s) failed to start
    ;
}
