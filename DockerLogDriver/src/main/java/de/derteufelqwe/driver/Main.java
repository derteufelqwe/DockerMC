package de.derteufelqwe.driver;

import lombok.SneakyThrows;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.net.URI;
import java.net.URL;
import java.nio.charset.StandardCharsets;

@Log4j2
public class Main {

    /*
     * ToDo:
     *  - Try to get ReadLogs working (https://docs.docker.com/engine/extend/plugins_logging/#logdriverreadlogs)
     */

    @SneakyThrows
    public static void main(String[] args) {
        DMCLogDriver driver = null;
//        File file = new File("/var/log/dmcdriver/count.txt");
//        file.createNewFile();
//        String input = FileUtils.readFileToString(file, StandardCharsets.UTF_8);
//        int number = 0;
//        if (!input.equals("")) {
//            number = Integer.parseInt(input);
//        }
//        FileUtils.write(file, Integer.toString(number + 1), StandardCharsets.UTF_8);

        try {
            driver = new DMCLogDriver();
            driver.addSignalHook();
            driver.startServer();
            log.info("Shutdown complete. Goodbye.");

        } catch (Exception e) {
            log.error("Failed to start LogDriver. Error:");
            log.error(ExceptionUtils.getMessage(e));
            driver.shutdown();
            System.exit(2);
        }
    }

}
