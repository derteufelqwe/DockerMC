package de.derteufelqwe.driver.endpoints;

import de.derteufelqwe.driver.messages.VolumeDriver;

import java.io.Serializable;

public class VolumeDriverRemoveEP extends Endpoint<VolumeDriver.RRemove, VolumeDriver.Remove> {

    public VolumeDriverRemoveEP(String data) {
        super(data);
    }

    @Override
    protected VolumeDriver.Remove process(VolumeDriver.RRemove request) {
        return new VolumeDriver.Remove();
    }

    @Override
    protected Class<? extends Serializable> getRequestType() {
        return VolumeDriver.RRemove.class;
    }

    @Override
    protected Class<? extends Serializable> getResponseType() {
        return VolumeDriver.Remove.class;
    }

}
