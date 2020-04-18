package de.derteufelqwe.ServerManager.config.configs.objects;

import com.github.dockerjava.api.model.Service;
import de.derteufelqwe.ServerManager.Utils;
import de.derteufelqwe.ServerManager.exceptions.FatalDockerMCError;
import de.derteufelqwe.commons.Constants;
import lombok.*;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;


@Data
@NoArgsConstructor
@AllArgsConstructor
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
public class BungeeProxy extends ServerBase {

    // Port of the proxy
    private int port;

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
        throw new FatalDockerMCError("Not implemented yet");
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

    // -----  Utility methods  -----

    private Map<String, String> getServiceLabels() {
        return Utils.quickLabel(Constants.ContainerType.BUNGEE);
    }

}
