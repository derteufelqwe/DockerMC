package de.derteufelqwe.ServerManager.setup;

import de.derteufelqwe.commons.Constants;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

/**
 * Response object of the creation of a Service like a {@link de.derteufelqwe.ServerManager.setup.servers.ServerPool}.
 */
@Getter
@ToString
public class ServiceCreateResponse {

    @Setter
    private String serviceName;
    private Constants.ContainerType type;
    @Setter
    private ServiceStartResult result;

    public ServiceCreateResponse(String serviceName, Constants.ContainerType type, ServiceStartResult result) {
        this.serviceName = serviceName;
        this.type = type;
        this.result = result;
    }

    public ServiceCreateResponse(String serviceName, Constants.ContainerType type) {
        this(serviceName, type, ServiceStartResult.UNKOWN);
    }

    public ServiceCreateResponse(Constants.ContainerType type) {
        this(null, type, ServiceStartResult.UNKOWN);
    }

}
