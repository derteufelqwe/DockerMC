package de.derteufelqwe.ServerManager.setup.infrastructure;

import com.github.dockerjava.api.command.CreateNetworkResponse;
import com.github.dockerjava.api.exception.NotFoundException;
import com.github.dockerjava.api.model.Network;
import de.derteufelqwe.ServerManager.Docker;
import de.derteufelqwe.ServerManager.exceptions.FatalDockerMCError;
import de.derteufelqwe.ServerManager.setup.NetworkTemplate;
import de.derteufelqwe.commons.Constants;

import java.util.List;
import java.util.concurrent.TimeUnit;

public class OvernetNetwork extends NetworkTemplate {

    public OvernetNetwork(Docker docker) {
        super(docker);
    }


    @Override
    public FindResponse find() {
        List<Network> networks = docker.getDocker().listNetworksCmd()
                .withNameFilter(Constants.NETW_OVERNET_NAME)
                .exec();

        if (networks.size() > 1) {
            throw new FatalDockerMCError("Found multiple overnet networks.");

        } else if (networks.size() == 1) {
            return new FindResponse(true, networks.get(0).getId());

        } else {
            return new FindResponse(false, null);
        }
    }

    @Override
    public CreateResponse create() {
        Network.Ipam networkSettings = new Network.Ipam()
                .withConfig(new Network.Ipam.Config()
                        .withSubnet(Constants.SUBNET_OVERNET));

        CreateNetworkResponse response = docker.getDocker().createNetworkCmd()
                .withDriver("overlay")
                .withIpam(networkSettings)
                .withName(Constants.NETW_OVERNET_NAME)
                .withAttachable(true)
                .exec();

        String networkID = response.getId();

        // Check if network exists
        try {
            TimeUnit.SECONDS.sleep(NETWORK_CREATE_DELAY);

            if (this.find().isFound()) {
                return new CreateResponse(true, networkID);
            }

        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        return new CreateResponse(false, networkID);
    }

    @Override
    public DestroyResponse destroy() {
        FindResponse findResponse = this.find();
        if (findResponse.isFound()) {
            docker.getDocker().removeNetworkCmd(findResponse.getServiceID()).exec();

            return new DestroyResponse(true, findResponse.getServiceID());
        }

        return new DestroyResponse(false, null);
    }

}
