package de.derteufelqwe.commons.logger;

import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;


/**
 * A wrapper for the default logger, which filters log messages and forwards the to the default logger if the shall be
 * logged. This logger is used for DMC logs only.
 */
public class DMCLogger {

    private Logger logger;
    private Level level;


    public DMCLogger(String name, Level level, Logger defaultLogger) {
        this.level = level;
        this.logger = Logger.getLogger(name);
        this.logger.setUseParentHandlers(false);

        Handler handler = new ForwardHandler(defaultLogger);
        handler.setLevel(level);
        this.logger.addHandler(handler);
    }


    public void setLevel(Level level) {
        this.level = level;
        for (Handler handler : this.logger.getHandlers()) {
            handler.setLevel(level);
        }
    }

    public Level getLevel() {
        return this.level;
    }


    public void severe(String msg, Object... args) {
        this.logger.severe(String.format(msg, args));
    }

    public void error(String msg, Object... args) {
        this.severe(msg, args);
    }

    public void warning(String msg, Object... args) {
        this.logger.warning(String.format(msg, args));
    }

    public void info(String msg, Object... args) {
        this.logger.info(String.format(msg, args));
    }

    public void config(String msg, Object... args) {
        this.logger.config(String.format(msg, args));
    }

    public void fine(String msg, Object... args) {
        this.logger.fine(String.format(msg, args));
    }

    public void finer(String msg, Object... args) {
        this.logger.finer(String.format(msg, args));
    }

    public void finest(String msg, Object... args) {
        this.logger.finest(String.format(msg, args));
    }

}
