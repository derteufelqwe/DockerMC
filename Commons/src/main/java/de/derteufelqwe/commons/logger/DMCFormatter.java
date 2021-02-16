package de.derteufelqwe.commons.logger;

import java.util.logging.Formatter;
import java.util.logging.LogRecord;

public class DMCFormatter extends Formatter {

    @Override
    public String format(LogRecord record) {
        return record.getMessage();
    }
}
