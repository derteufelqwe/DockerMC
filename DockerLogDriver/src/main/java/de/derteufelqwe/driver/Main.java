package de.derteufelqwe.driver;

import lombok.SneakyThrows;
import lombok.extern.log4j.Log4j2;

@Log4j2
public class Main {

    /*
     * ToDo:
     *  - Try to get ReadLogs working (https://docs.docker.com/engine/extend/plugins_logging/#logdriverreadlogs)
     */

    @SneakyThrows
    public static void main(String[] args) {
        DMCLogDriver driver = null;

        try {
            driver = new DMCLogDriver();
            driver.addSignalHook();
            driver.startServer();
            log.info("Shutdown complete. Goodbye.");

        } catch (Exception e) {
            log.error("Failed to start LogDriver. Error: ", e);
            driver.shutdown();
            System.exit(2);
        }
    }

}
