package de.derteufelqwe.ServerManager;

import javax.annotation.CheckForNull;
import com.github.dockerjava.api.model.Container;
import de.derteufelqwe.ServerManager.exceptions.FatalDockerMCError;
import de.derteufelqwe.commons.Constants;

import java.awt.*;
import java.util.List;

/**
 * Utility class to get the DNS, Registry and API-proxy containers
 */
public class ContainerGetter {

    private Docker docker;

    public ContainerGetter() {
        this.docker = ServerManager.getDocker();
    }


    @CheckForNull
    public Container getDNSContainer() {

        List<Container> containerList = docker.getDocker().listContainersCmd()
                .withLabelFilter(Utils.quickLabel(Constants.ContainerType.DNS))
                .exec();

        if (containerList.size() > 1) {
            throw new FatalDockerMCError("Found multiple DNS containers.");

        } else if (containerList.size() == 1) {
            return containerList.get(0);

        } else {
            return null;
        }
    }

    @CheckForNull
    public Container getRegistryContainer() {

        List<Container> containerList = docker.getDocker().listContainersCmd()
                .withLabelFilter(Utils.quickLabel(Constants.ContainerType.REGISTRY))
                .exec();

        if (containerList.size() > 1) {
            throw new FatalDockerMCError("Found multiple Registry containers.");

        } else if (containerList.size() == 1) {
            return containerList.get(0);

        } else {
            return null;
        }
    }

    @CheckForNull
    public Container getAPIProxyContainer() {

        List<Container> containerList = docker.getDocker().listContainersCmd()
                .withLabelFilter(Utils.quickLabel(Constants.ContainerType.API_PROXY))
                .exec();

        if (containerList.size() > 1) {
            throw new FatalDockerMCError("Found multiple API-proxy containers.");

        } else if (containerList.size() == 1) {
            return containerList.get(0);

        } else {
            return null;
        }
    }

}
