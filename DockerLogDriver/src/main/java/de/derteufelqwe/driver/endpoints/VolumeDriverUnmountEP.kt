package de.derteufelqwe.driver.endpoints;

import de.derteufelqwe.driver.messages.VolumeDriver;

import java.io.Serializable;

public class VolumeDriverUnmountEP extends Endpoint<VolumeDriver.RUnmount, VolumeDriver.Unmount> {

    public VolumeDriverUnmountEP(String data) {
        super(data);
    }

    @Override
    protected VolumeDriver.Unmount process(VolumeDriver.RUnmount request) {
        return new VolumeDriver.Unmount();
    }

    @Override
    protected Class<? extends Serializable> getRequestType() {
        return VolumeDriver.RUnmount.class;
    }

    @Override
    protected Class<? extends Serializable> getResponseType() {
        return VolumeDriver.Unmount.class;
    }

}
