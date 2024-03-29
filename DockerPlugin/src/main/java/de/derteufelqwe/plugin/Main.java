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
            String dbHost = parseDBHost();
            String dbPassword = parseDBPassword();

            driver = new DMCLogDriver(dbHost, dbPassword);
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
            try {
                Scanner scanner = new Scanner(System.in);
                String input = scanner.next();
                if (input.equals("stop")) {
                    log.warn("RECEIVED STOP COMMAND.");
                    dmcLogDriver.shutdown();
                }

            } catch (Exception e) {
                log.debug("Failed to start system stopper.");
            }
        }).start();
    }


    private static void parseLogLevel() {
        String logLevel = System.getenv("LOG_LEVEL");
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
        String dbHost = System.getenv("DB_HOST");
        if (dbHost == null || dbHost.equals("")) {
            log.warn("DB_HOST not set. Using localhost instead");
            return "127.0.0.1";
        }

        return dbHost;
    }

    private static String parseDBPassword() {
        String dbPassword = System.getenv("DB_PASSWORD");
        if (dbPassword == null || dbPassword.equals("")) {
            throw new IllegalArgumentException("DB_PASSWORD not set.");
        }

        return dbPassword;
    }

}
