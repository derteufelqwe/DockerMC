package de.derteufelqwe.plugin.endpoints;

import de.derteufelqwe.commons.hibernate.SessionBuilder;
import de.derteufelqwe.commons.hibernate.objects.NWContainer;
import de.derteufelqwe.plugin.DMCLogDriver;
import de.derteufelqwe.plugin.log.LogDownloadEntry;
import de.derteufelqwe.plugin.messages.LogDriver;
import lombok.extern.log4j.Log4j2;

import java.io.Serializable;
import java.sql.Timestamp;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Log4j2
public class LogDriverStopLoggingEP extends Endpoint<LogDriver.RStopLogging, LogDriver.StopLogging> {

    private final Map<String, LogDownloadEntry> logfileConsumers = DMCLogDriver.getLogfileConsumers();
    private final SessionBuilder sessionBuilder = DMCLogDriver.getSessionBuilder();


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
            log.error(error);
            return new LogDriver.StopLogging(error);
        }

        // Wait until the last log read is 2000ms ago
        while ((System.currentTimeMillis() - entry.getConsumer().getLastLogReadTime().get()) <= DMCLogDriver.FINISH_LOG_READ_DELAY) {
            try {
                TimeUnit.MILLISECONDS.sleep(250);
            } catch (Exception ignored) {
            }
        }

        finishNWContainerInDB(entry.getConsumer().getContainerID());

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

    // -----  Utility methods  -----

    /**
     * If a NWContainer is present with this ID, updates its stop time
     *
     * @param containerID
     */
    private void finishNWContainerInDB(String containerID) {
        sessionBuilder.execute(session -> {
            NWContainer nwContainer = session.get(NWContainer.class, containerID);
            if (nwContainer == null) {
                return;
            }

            nwContainer.setStopTime(new Timestamp(System.currentTimeMillis()));

            session.update(nwContainer);
        });
    }

}
