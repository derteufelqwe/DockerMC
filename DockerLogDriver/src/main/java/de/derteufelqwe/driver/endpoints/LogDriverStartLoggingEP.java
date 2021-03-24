package de.derteufelqwe.driver.endpoints;

import de.derteufelqwe.driver.DMCLogDriver;
import de.derteufelqwe.driver.LogConsumer;
import de.derteufelqwe.driver.messages.LogDriver;

import java.io.Serializable;
import java.util.Map;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class LogDriverStartLoggingEP extends Endpoint<LogDriver.RStartLogging, LogDriver.StartLogging> {

    private ThreadPoolExecutor threadPool = DMCLogDriver.getThreadPool();
    private Map<String, Future<?>> containerFutures = DMCLogDriver.getLogfileFutures();


    public LogDriverStartLoggingEP(String data) {
        super(data);
    }

    @Override
    protected LogDriver.StartLogging process(LogDriver.RStartLogging request) {
        String file = request.getFile();
        String containerID = request.getInfo().getContainerID();

        LogConsumer logConsumer = new LogConsumer(file, containerID);
        Future<?> future = threadPool.submit(logConsumer);
        containerFutures.put(file, future);

        try {
            TimeUnit.MILLISECONDS.sleep(250);

        } catch (InterruptedException e) {
            System.err.println("StartLogging sleep interrupted.");
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
