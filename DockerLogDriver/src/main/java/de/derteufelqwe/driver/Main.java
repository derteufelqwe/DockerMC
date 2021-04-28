package de.derteufelqwe.driver;

import lombok.SneakyThrows;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.config.Configurator;

import java.io.Console;
import java.util.Scanner;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.ConsoleHandler;

@Log4j2
public class Main {

    /*
     * ToDo:
     *  - Try to get ReadLogs working (https://docs.docker.com/engine/extend/plugins_logging/#logdriverreadlogs)
     */

    /*
     * Notes:
     *  - Docker will restart the plugin if it stops.
     */

    @SneakyThrows
    public static void main(String[] args) {
        String logLevel = System.getProperty("LOG_LEVEL");
        logLevel = logLevel != null ? logLevel.toUpperCase() : "";

        try {
            Level level = Level.valueOf(logLevel);
            Configurator.setAllLevels("de.derteufelqwe", level);
            log.info("Changed log level to {}.", level);

        } catch (IllegalArgumentException e) {
            log.warn("Invalid log level {}. Using default level INFO.", logLevel);
        }

        DMCLogDriver driver = null;
        try {
            driver = new DMCLogDriver();
            driver.addSignalHook();
            startStopper(driver);
            driver.startServer();
            driver.shutdown();

        } catch (Exception e) {
            log.error("Failed to start LogDriver. ", e);
            if (driver != null) {
                driver.stopWebserver();
            }
            System.exit(2);
        }
    }


    private static void startStopper(DMCLogDriver dmcLogDriver) {
        log.warn("[DEBUG FEATURE] System stopper enabled");
        new Thread(() -> {
            Scanner scanner = new Scanner(System.in);
            String input = scanner.next();
            if (input.equals("stop")) {
                log.warn("RECEIVED STOP COMMAND.");
                dmcLogDriver.shutdown();
            }
        }).start();
    }

}
