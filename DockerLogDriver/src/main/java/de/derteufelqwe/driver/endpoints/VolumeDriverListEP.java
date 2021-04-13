package de.derteufelqwe.driver.endpoints;

import de.derteufelqwe.driver.messages.VolumeDriver;

import java.io.Serializable;

public class VolumeDriverListEP extends Endpoint<VolumeDriver.RList, VolumeDriver.List> {

    public VolumeDriverListEP(String data) {
        super(data);
    }

    @Override
    protected VolumeDriver.List process(VolumeDriver.RList request) {
        return new VolumeDriver.List();
    }

    @Override
    protected Class<? extends Serializable> getRequestType() {
        return VolumeDriver.RList.class;
    }

    @Override
    protected Class<? extends Serializable> getResponseType() {
        return VolumeDriver.List.class;
    }

}
