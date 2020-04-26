package de.derteufelqwe.ServerManager.setup;

import de.derteufelqwe.ServerManager.Docker;

public class NetworkTemplate extends DockerObjTemplate {

    protected final int NETWORK_CREATE_DELAY = 2;     // Time for networks to get up and running

    private NetworkTemplate(String image, String ramLimit, String cpuLimit) {
        super(image, ramLimit, cpuLimit);
    }

    public NetworkTemplate(Docker docker) {
        super(docker);
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



}
