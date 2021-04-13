package de.derteufelqwe.driver.endpoints;

import de.derteufelqwe.driver.messages.VolumeDriver;

import java.io.Serializable;

public class VolumeDriverCreateEP extends Endpoint<VolumeDriver.RCreate, VolumeDriver.Create> {

    public VolumeDriverCreateEP(String data) {
        super(data);
    }

    @Override
    protected VolumeDriver.Create process(VolumeDriver.RCreate request) {
        return new VolumeDriver.Create();
    }

    @Override
    protected Class<? extends Serializable> getRequestType() {
        return VolumeDriver.RCreate.class;
    }

    @Override
    protected Class<? extends Serializable> getResponseType() {
        return VolumeDriver.Create.class;
    }

}
