package de.derteufelqwe.driver.endpoints;

import de.derteufelqwe.driver.DMCLogDriver;
import de.derteufelqwe.driver.messages.LogDriver;

import java.io.Serializable;
import java.util.Map;
import java.util.concurrent.Future;

public class LogDriverStopLoggingEP extends Endpoint<LogDriver.RStopLogging, LogDriver.StopLogging> {

    private Map<String, Future<?>> logfileFutures = DMCLogDriver.getLogfileFutures();


    public LogDriverStopLoggingEP(String data) {
        super(data);
    }

    @Override
    protected LogDriver.StopLogging process(LogDriver.RStopLogging request) {
        Future<?> future = logfileFutures.get(request.getFile());

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
