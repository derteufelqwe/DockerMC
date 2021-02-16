package de.derteufelqwe.commons.logger;

import org.checkerframework.checker.units.qual.C;

import java.util.logging.ConsoleHandler;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;


public class DMCLogger {

    private Logger logger;


    public DMCLogger(String name, Level level) {
        this.logger = Logger.getLogger(name);
        this.logger.setUseParentHandlers(false);

        Handler handler = new ConsoleHandler();
        handler.setLevel(level);
        handler.setFormatter(new DMCFormatter());
        this.logger.addHandler(handler);
    }


    public void setLevel(Level level) {
        for (Handler handler : this.logger.getHandlers()) {
            handler.setLevel(level);
        }
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
