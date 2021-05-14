package de.derteufelqwe.ServerManager.setup.templates;

import com.github.dockerjava.api.command.CreateNetworkResponse;
import com.github.dockerjava.api.model.Network;
import de.derteufelqwe.ServerManager.Docker;
import de.derteufelqwe.ServerManager.exceptions.FatalDockerMCError;
import de.derteufelqwe.commons.Constants;
import de.derteufelqwe.commons.config.annotations.Exclude;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import org.apache.commons.lang.StringUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class DockerNetwTemplate {

    private final int NETWORK_CREATE_DELAY = 2;     // Time for networks to get up and running

    @Getter(AccessLevel.NONE)
    @Setter(AccessLevel.NONE)
    @Exclude
    protected Docker docker;

    /**
     * Name of the network in docker
     */
    protected String name;

    public DockerNetwTemplate(String name) {
        this.name = name;
    }


    /**
     * Initialize the instance with a working Docker instance.
     * If this method doesn't get called before executing any other methods, the other methods will fail.
     *
     * @param docker Docker instance to set
     */
    public void init(Docker docker) {
        this.docker = docker;
    }

    /**
     * Tries to find an existing network based on the name
     */
    public DockerObjTemplate.FindResponse find() {
        List<Network> networks = docker.getDocker().listNetworksCmd()
                .withNameFilter(Constants.NETW_OVERNET_NAME)
                .exec();

        if (networks.size() > 1) {
            throw new FatalDockerMCError("Found multiple networks named " + this.name + ".");

        } else if (networks.size() == 1) {
            return new DockerObjTemplate.FindResponse(true, networks.get(0).getId());

        } else {
            return new DockerObjTemplate.FindResponse(false, null);
        }
    }

    /**
     * Tries to create the network
     */
    public DockerObjTemplate.CreateResponse create() {

        CreateNetworkResponse response = docker.getDocker().createNetworkCmd()
                .withDriver(this.getDriver())
                .withIpam(this.getIpamSettings())
                .withName(this.name) // NETW_OVERNET_NAME
                .withAttachable(this.getAttachable())
                .withOptions(this.getOptions())
                .exec();

        String networkID = response.getId();

        // Check if network exists
        try {
            TimeUnit.SECONDS.sleep(NETWORK_CREATE_DELAY);

            if (this.find().isFound()) {
                return new DockerObjTemplate.CreateResponse(true, networkID);
            }

        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        return new DockerObjTemplate.CreateResponse(false, networkID);
    }

    /**
     * Tries to destroy the network, if it exists
     */
    public DockerObjTemplate.DestroyResponse destroy() {
        DockerObjTemplate.FindResponse findResponse = this.find();
        if (findResponse.isFound()) {
            docker.getDocker().removeNetworkCmd(findResponse.getServiceID()).exec();

            return new DockerObjTemplate.DestroyResponse(true, findResponse.getServiceID());
        }

        return new DockerObjTemplate.DestroyResponse(false, null);
    }


    // -----  Other methods  -----

    /**
     * Basic validation if parameters are not null.
     *
     * @return List with all parameter names that are null.
     */
    protected List<String> findNullParams() {
        List<String> resultList = new ArrayList<>();

        if (this.name == null) {
            resultList.add("name");
        }

        return resultList;
    }


    // -----  Creation methods  -----

    /**
     * Returns the driver like bridge or overlay
     */
    protected String getDriver() {
        return "bridge";
    }

    /**
     * Returns the IpamSettings like the subnet
     */
    private Network.Ipam getIpamSettings() {
        return new Network.Ipam()
                .withConfig(new Network.Ipam.Config()
                        .withSubnet(this.getSubnet()));
    }

    /**
     * Returns the subnet like 10.1.2.0
     */
    protected String getSubnet() {
        return null;
    }

    /**
     * Returns if the network is attachable
     */
    protected boolean getAttachable() {
        return false;
    }

    /**
     * Returns the options
     */
    protected Map<String, String> getOptions() {
        return new HashMap<>();
    }

}
