package de.derteufelqwe.plugin;

import lombok.SneakyThrows;
import lombok.extern.log4j.Log4j2;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.config.Configurator;

import java.io.File;
import java.util.Arrays;
import java.util.Scanner;

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
        parseLogLevel();

        DMCLogDriver driver = null;
        try {
//            String dbHost = parseDBHost();
//            String dbPassword = parseDBPassword();
            driver = new DMCLogDriver("ubuntu1", "admin");
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


    private static void parseLogLevel() {
        String logLevel = System.getProperty("LOG_LEVEL");
        logLevel = logLevel != null ? logLevel.toUpperCase() : "";

        try {
            Level level = Level.valueOf(logLevel);
            Configurator.setAllLevels("de.derteufelqwe", level);
            log.info("Changed log level to {}.", level);

        } catch (IllegalArgumentException e) {
            log.warn("Invalid log level '{}'. Using default level INFO.", logLevel);
        }
    }

    private static String parseDBHost() {
        String dbHost = System.getProperty("DB_HOST");
        log.debug(System.getProperties());
        log.info("Host: {}", dbHost);
        if (dbHost == null || dbHost.equals("")) {
            throw new IllegalArgumentException("DB_HOST not set.");
        }

        return dbHost;
    }

    private static String parseDBPassword() {
        String dbPassword = System.getProperty("DB_PASSWORD");
        if (dbPassword == null || dbPassword.equals("")) {
            throw new IllegalArgumentException("DB_PASSWORD not set.");
        }

        return dbPassword;
    }

}
