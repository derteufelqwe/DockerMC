package de.derteufelqwe.plugin.endpoints;

import de.derteufelqwe.plugin.messages.LogDriver;

import java.io.Serializable;

public class LogDriverCapabilitiesEP extends Endpoint<LogDriver.RCapabilities, LogDriver.Capabilities> {

    public LogDriverCapabilitiesEP(String data) {
        super(data);
    }

    @Override
    protected LogDriver.Capabilities process(LogDriver.RCapabilities request) {
        return new LogDriver.Capabilities();
    }

    @Override
    protected Class<? extends Serializable> getRequestType() {
        return LogDriver.RCapabilities.class;
    }

    @Override
    protected Class<? extends Serializable> getResponseType() {
        return LogDriver.Capabilities.class;
    }
}
