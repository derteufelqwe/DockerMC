package de.derteufelqwe.driver;

import lombok.SneakyThrows;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.config.Configurator;

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
            driver.startServer();
            driver.shutdown();

        } catch (Exception e) {
            log.error("Failed to start LogDriver. ", e);
            if (driver != null) {
                driver.shutdown();
            }
            System.exit(2);
        }
    }

}
