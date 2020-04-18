package de.derteufelqwe.ServerManager.config.configs.objects;

import com.github.dockerjava.api.command.CreateServiceResponse;
import com.github.dockerjava.api.model.*;
import de.derteufelqwe.ServerManager.Utils;
import de.derteufelqwe.ServerManager.exceptions.FatalDockerMCError;
import de.derteufelqwe.commons.Constants;
import lombok.*;
import org.apache.commons.lang.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Data
@NoArgsConstructor
@AllArgsConstructor
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
public class ServerPool extends ServerBase {

    // Soft playerlimit
    private int softPlayerLimit;


    @Override
    public ValidationResponse valid() {
        ValidationResponse response = new ValidationResponse(true, this.name, "");
        List<String> nullParams = this.validateParamsNotNull();

        if (this.softPlayerLimit == 0) {
            nullParams.add("softPlayerLimit");
        }

        if (nullParams.size() != 0) {
            response.setValid(false);
            response.setReason(response.getReason() +
                    "Parameters " + StringUtils.join(nullParams, ", ") + " can't be null.\n"
            );
        }

        try {
            Utils.convertMemoryString(this.ramLimit);

        } catch (FatalDockerMCError e) {
            response.setValid(false);
            response.setReason(response.getReason() +
                    "Ram constraint value " + this.ramLimit + " is unkown.\n"
            );
        }

        return response;
    }

    @Override
    public FindResponse find() {
        List<Service> services = this.docker.getDocker().listServicesCmd()
                .withLabelFilter(this.getServiceLabels())
                .exec();

        if (services.size() > 1) {
            throw new FatalDockerMCError("Found multiple services %s for the config %s.",
                    services.stream().map(Service::getId).collect(Collectors.joining(", ")), this.name);

        } else if (services.size() == 1) {
            return new ServerBase.FindResponse(true, services.get(0).getId());

        } else {
            return new ServerBase.FindResponse(false, null);
        }
    }

    @Override
    public CreateResponse create() {
        ServiceSpec serviceSpec = this.getServiceSpec();

        CreateServiceResponse serviceResponse = docker.getDocker().createServiceCmd(serviceSpec)
                .withAuthConfig(this.authConfig)
                .exec();

        return new ServerBase.CreateResponse(true, serviceResponse.getId());
    }

    @Override
    public DestroyResponse destroy() {
        FindResponse findResponse = this.find();

        if (findResponse.isFound()) {
            this.docker.getDocker().removeServiceCmd(findResponse.getServiceID()).exec();
            return new ServerBase.DestroyResponse(true, findResponse.getServiceID());
        }

        return new ServerBase.DestroyResponse(false, null);
    }


    @Override
    protected Map<String, String> getContainerLabels() {
        Map<String, String> containerLabels = Utils.quickLabel(Constants.ContainerType.MINECRAFT);
        containerLabels.put(Constants.SERVER_NAME_KEY, this.name);

        return containerLabels;
    }

    @Override
    protected Map<String, String> getServiceLabels() {
        Map<String, String> serviceLabels = Utils.quickLabel(Constants.ContainerType.MINECRAFT_POOL);
        serviceLabels.put(Constants.SERVER_NAME_KEY, this.name);

        return serviceLabels;
    }
}
