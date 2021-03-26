package de.derteufelqwe.driver.endpoints;

import de.derteufelqwe.driver.DMCLogDriver;
import de.derteufelqwe.driver.workers.LogConsumer;
import de.derteufelqwe.driver.workers.LogDownloadEntry;
import de.derteufelqwe.driver.messages.LogDriver;
import lombok.extern.log4j.Log4j2;

import java.io.Serializable;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

@Log4j2
public class LogDriverStartLoggingEP extends Endpoint<LogDriver.RStartLogging, LogDriver.StartLogging> {

    private final ExecutorService threadPool = DMCLogDriver.getThreadPool();
    private final Map<String, LogDownloadEntry> logfileConsumers = DMCLogDriver.getLogfileConsumers();


    public LogDriverStartLoggingEP(String data) {
        super(data);
    }

    @Override
    protected LogDriver.StartLogging process(LogDriver.RStartLogging request) {
        String file = request.getFile();
        String containerID = request.getInfo().getContainerID();

        LogConsumer logConsumer = new LogConsumer(file, containerID);
        Future<?> future = threadPool.submit(logConsumer);
        logfileConsumers.put(file, new LogDownloadEntry(logConsumer, future));

        try {
            TimeUnit.MILLISECONDS.sleep(250);

        } catch (InterruptedException e) {
            log.error("StartLogging sleep interrupted.");
        }

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
