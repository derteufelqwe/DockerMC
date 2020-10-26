package de.derteufelqwe.ServerManager.setup;

import lombok.Getter;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Response object of the creation of Services specified in a config like {@link de.derteufelqwe.ServerManager.config.InfrastructureConfig}
 */
public class ConfigCreateResponse {

    @Getter
    private List<ServiceCreateResponse> results = new ArrayList<>();

    public ConfigCreateResponse() {
    }

    /**
     * Adds a new Result
     */
    public void addResult(ServiceCreateResponse response) {
        this.results.add(response);
    }


    /**
     * Returns all results, which are of type error
     */
    public List<ServiceCreateResponse> getErrors() {
        return this.results.stream().filter(o -> o.getResult() == ServiceStartResult.FAILED_GENERIC).collect(Collectors.toList());
    }

    /**
     * Returns all results, which are not configured
     */
    public List<ServiceCreateResponse> getNotConfigured() {
        return this.results.stream().filter(o -> o.getResult() == ServiceStartResult.NOT_CONFIGURED).collect(Collectors.toList());
    }

    /**
     * Returns all results, which have no error
     */
    public List<ServiceCreateResponse> getSuccessful() {
        return this.results.stream().filter(o -> o.getResult() == ServiceStartResult.OK || o.getResult() == ServiceStartResult.RUNNING).collect(Collectors.toList());
    }

}
