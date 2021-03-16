package de.derteufelqwe.ServerManager.setup;

import de.derteufelqwe.commons.Constants;
import lombok.Data;

/**
 * Response object of the destruction of a Service like a {@link de.derteufelqwe.ServerManager.setup.servers.ServerPool}.
 */
@Data
public class ServiceStopResponse {

    private String serviceName;
    private Constants.ContainerType type;
    private ServiceStop result;
    private String serviceId;
    private String additionalInfos = "";

    public ServiceStopResponse(String serviceName, Constants.ContainerType type, ServiceStop result) {
        this.serviceName = serviceName;
        this.type = type;
        this.result = result;
    }

    public ServiceStopResponse(String serviceName, Constants.ContainerType type) {
        this(serviceName, type, ServiceStop.UNKNOWN);
    }

    public ServiceStopResponse(Constants.ContainerType type) {
        this(null, type, ServiceStop.UNKNOWN);
    }

}
