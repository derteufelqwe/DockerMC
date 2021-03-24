package de.derteufelqwe.driver.endpoints;

import de.derteufelqwe.driver.messages.LogDriver;

import java.io.Serializable;

public class LogDriverStopLoggingEP extends Endpoint<LogDriver.RStopLogging, LogDriver.StopLogging> {

    public LogDriverStopLoggingEP(String data) {
        super(data);
    }

    @Override
    protected LogDriver.StopLogging process(LogDriver.RStopLogging request) {
        return new LogDriver.StopLogging();
    }

    @Override
    protected Class<? extends Serializable> getRequestType() {
        return LogDriver.RStopLogging.class;
    }

    @Override
    protected Class<? extends Serializable> getResponseType() {
        return LogDriver.StopLogging.class;
    }
}
