package de.derteufelqwe.commons.logger;

import com.google.common.base.Strings;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.message.MessageFormatMessage;

import java.util.Map;
import java.util.MissingResourceException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Handler;
import java.util.logging.LogRecord;


class LogHandler extends Handler {

    private final Map<String, Logger> cache = new ConcurrentHashMap();

    public LogHandler() {

    }

    public void publish(LogRecord record) {
        if (this.isLoggable(record)) {
            Logger logger = this.cache.computeIfAbsent(Strings.nullToEmpty(record.getLoggerName()), LogManager::getLogger);

        }
    }

    public void flush() {
    }

    public void close() {
    }
}