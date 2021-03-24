package de.derteufelqwe.driver.endpoints;

import de.derteufelqwe.driver.messages.LogDriver;

import java.io.Serializable;

public class LogDriverStartLoggingEP extends Endpoint<LogDriver.RStartLogging, LogDriver.StartLogging> {

    public LogDriverStartLoggingEP(String data) {
        super(data);
    }

    @Override
    protected LogDriver.StartLogging process(LogDriver.RStartLogging request) {
        return new LogDriver.StartLogging();
    }

    @Override
    protected Class<? extends Serializable> getRequestType() {
        return LogDriver.RStartLogging.class;
    }

    @Override
    protected Class<? extends Serializable> getResponseType() {
        return LogDriver.StartLogging.class;
    }
}
