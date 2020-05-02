package de.derteufelqwe.ServerManager.setup.servers;

import de.derteufelqwe.ServerManager.Docker;
import de.derteufelqwe.ServerManager.setup.ServiceConstraints;
import lombok.*;

import java.util.List;
import java.util.Map;

@Data
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
public class PersistentServerPool extends ServerTemplate {

    // Soft playerlimit
    private int softPlayerLimit;

    public PersistentServerPool(String image, String ramLimit, String cpuLimit, String name, int replications, ServiceConstraints constraints, int softPlayerLimit) {
        super(image, ramLimit, cpuLimit, name, replications, constraints);
        this.softPlayerLimit = softPlayerLimit;
    }

    public PersistentServerPool(Docker docker) {
        super(docker);
    }

    @Override
    public ValidationResponse valid() {
        return null;
    }

    @Override
    public FindResponse find() {
        return null;
    }

    @Override
    public CreateResponse create() {
        return null;
    }

    @Override
    public DestroyResponse destroy() {
        return null;
    }


    @Override
    protected Map<String, String> getContainerLabels() {
        return null;
    }

    @Override
    protected Map<String, String> getServiceLabels() {
        return null;
    }
}
