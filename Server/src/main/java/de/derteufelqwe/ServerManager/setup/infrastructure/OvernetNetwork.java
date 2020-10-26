package de.derteufelqwe.ServerManager.setup.infrastructure;

import de.derteufelqwe.ServerManager.setup.templates.DockerNetwTemplate;
import de.derteufelqwe.commons.Constants;

public class OvernetNetwork extends DockerNetwTemplate {

    public OvernetNetwork() {
        super(Constants.NETW_OVERNET_NAME);
    }


    // -----  Creation methods  -----

    @Override
    protected String getDriver() {
        return "overlay";
    }

    @Override
    protected String getSubnet() {
        return Constants.SUBNET_OVERNET;
    }

    @Override
    protected boolean getAttachable() {
        return true;
    }

}
