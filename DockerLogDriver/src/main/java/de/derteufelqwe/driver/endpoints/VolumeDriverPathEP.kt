package de.derteufelqwe.driver.endpoints;

import de.derteufelqwe.driver.messages.VolumeDriver;

import java.io.Serializable;

public class VolumeDriverPathEP extends Endpoint<VolumeDriver.RPath, VolumeDriver.Path> {

    public VolumeDriverPathEP(String data) {
        super(data);
    }

    @Override
    protected VolumeDriver.Path process(VolumeDriver.RPath request) {
        return new VolumeDriver.Path();
    }

    @Override
    protected Class<? extends Serializable> getRequestType() {
        return VolumeDriver.RPath.class;
    }

    @Override
    protected Class<? extends Serializable> getResponseType() {
        return VolumeDriver.Path.class;
    }

}
