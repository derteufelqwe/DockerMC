package de.derteufelqwe.driver.endpoints;

import de.derteufelqwe.driver.messages.VolumeDriver;

import java.io.Serializable;
import java.util.ArrayList;

public class VolumeDriverGetEP extends Endpoint<VolumeDriver.RGet, VolumeDriver.Get> {

    public VolumeDriverGetEP(String data) {
        super(data);
    }

    @Override
    protected VolumeDriver.Get process(VolumeDriver.RGet request) {
        return new VolumeDriver.Get(new VolumeDriver.Volume(), "Get error");
    }

    @Override
    protected Class<? extends Serializable> getRequestType() {
        return VolumeDriver.RGet.class;
    }

    @Override
    protected Class<? extends Serializable> getResponseType() {
        return VolumeDriver.Get.class;
    }

}
