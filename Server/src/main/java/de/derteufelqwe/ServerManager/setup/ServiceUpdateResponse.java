package de.derteufelqwe.ServerManager.setup;

import de.derteufelqwe.commons.Constants;
import lombok.Data;

/**
 * Response object of the update of a Service like a {@link de.derteufelqwe.ServerManager.setup.servers.ServerPool}.
 */

@Data
public class ServiceUpdateResponse {

    private String serviceName;
    private Constants.ContainerType type;
    private ServiceUpdate result;
    private String serviceId;

    public ServiceUpdateResponse(String serviceName, Constants.ContainerType type, ServiceUpdate result) {
        this.serviceName = serviceName;
        this.type = type;
        this.result = result;
    }

    public ServiceUpdateResponse(String serviceName, Constants.ContainerType type) {
        this(serviceName, type, ServiceUpdate.UNKNOWN);
    }

    public ServiceUpdateResponse(Constants.ContainerType type) {
        this(null, type, ServiceUpdate.UNKNOWN);
    }

    public ServiceUpdateResponse(Constants.ContainerType type, ServiceUpdate result) {
        this(null, type, result);
    }


}
