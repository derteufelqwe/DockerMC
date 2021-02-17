package de.derteufelqwe.commons.logger;

import java.util.logging.Handler;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

public class ForwardHandler extends Handler {

    private Logger logger;

    public ForwardHandler(Logger logger) {
        this.logger = logger;
    }


    @Override
    public void publish(LogRecord record) {
        if (!this.isLoggable(record))
            return;

        String message = record.getMessage();

        switch (record.getLevel().getName()) {
            case "OFF":
                break;

            case "SEVERE":
                this.logger.severe(message);
                break;

            case "WARNING":
                this.logger.warning(message);
                break;

            default:
                this.logger.info(message);
                break;
        }
    }

    @Override
    public void flush() {

    }

    @Override
    public void close() throws SecurityException {

    }
}
