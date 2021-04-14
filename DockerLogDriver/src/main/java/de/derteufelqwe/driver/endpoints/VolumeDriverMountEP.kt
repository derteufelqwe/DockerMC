package de.derteufelqwe.driver.endpoints;

import de.derteufelqwe.driver.messages.VolumeDriver;

import java.io.Serializable;

public class VolumeDriverMountEP extends Endpoint<VolumeDriver.RMount, VolumeDriver.Mount> {

    public VolumeDriverMountEP(String data) {
        super(data);
    }

    @Override
    protected VolumeDriver.Mount process(VolumeDriver.RMount request) {
        return new VolumeDriver.Mount();
    }

    @Override
    protected Class<? extends Serializable> getRequestType() {
        return VolumeDriver.RMount.class;
    }

    @Override
    protected Class<? extends Serializable> getResponseType() {
        return VolumeDriver.Mount.class;
    }

}
