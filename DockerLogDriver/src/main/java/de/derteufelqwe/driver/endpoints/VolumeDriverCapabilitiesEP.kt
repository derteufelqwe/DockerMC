package de.derteufelqwe.driver.endpoints;

import de.derteufelqwe.driver.messages.VolumeDriver;

import java.io.Serializable;

public class VolumeDriverCapabilitiesEP extends Endpoint<VolumeDriver.RCapabilities, VolumeDriver.Capabilities> {

    public VolumeDriverCapabilitiesEP(String data) {
        super(data);
    }

    @Override
    protected VolumeDriver.Capabilities process(VolumeDriver.RCapabilities request) {
        return new VolumeDriver.Capabilities();
    }

    @Override
    protected Class<? extends Serializable> getRequestType() {
        return VolumeDriver.RCapabilities.class;
    }

    @Override
    protected Class<? extends Serializable> getResponseType() {
        return VolumeDriver.Capabilities.class;
    }
}
