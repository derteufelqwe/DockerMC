package de.derteufelqwe.ServerManager.setup;

import de.derteufelqwe.commons.Constants;
import lombok.Data;

/**
 * Response object of the creation of a Service like a {@link de.derteufelqwe.ServerManager.setup.servers.ServerPool}.
 */
@Data
public class ServiceCreateResponse {

    private String serviceName;
    private Constants.ContainerType type;
    private ServiceStart result;
    private String serviceId;
    private String additionalInfos = "";
    private boolean updated = false;

    public ServiceCreateResponse(String serviceName, Constants.ContainerType type, ServiceStart result) {
        this.serviceName = serviceName;
        this.type = type;
        this.result = result;
    }

    public ServiceCreateResponse(String serviceName, Constants.ContainerType type) {
        this(serviceName, type, ServiceStart.UNKNOWN);
    }

    public ServiceCreateResponse(Constants.ContainerType type) {
        this(null, type, ServiceStart.UNKNOWN);
    }

}
