package de.derteufelqwe.driver.endpoints;

import de.derteufelqwe.driver.DMCLogDriver;
import de.derteufelqwe.driver.LogDownloadEntry;
import de.derteufelqwe.driver.messages.LogDriver;

import java.io.Serializable;
import java.util.Map;
import java.util.concurrent.*;

public class LogDriverStopLoggingEP extends Endpoint<LogDriver.RStopLogging, LogDriver.StopLogging> {

    private final Map<String, LogDownloadEntry> logfileConsumers = DMCLogDriver.getLogfileConsumers();


    public LogDriverStopLoggingEP(String data) {
        super(data);
    }


    @Override
    protected LogDriver.StopLogging process(LogDriver.RStopLogging request) {
        String file = request.getFile();
        String error = "";
        LogDownloadEntry entry = logfileConsumers.get(file);

        if (entry == null) {
            error = String.format("Failed to find LogDownloadEntry for %s.%n", file);
            System.err.println(error);

        } else {
            // Wait until the last log read is 2000ms ago
            while ((System.currentTimeMillis() - entry.getConsumer().getLastLogReadTime().get()) <= DMCLogDriver.FINISH_LOG_READ_DELAY) {
                try {
                    TimeUnit.MILLISECONDS.sleep(250);
                } catch (Exception ignored) {}
            }
        }

        return new LogDriver.StopLogging(error);
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
