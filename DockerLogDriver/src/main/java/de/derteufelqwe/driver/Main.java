package de.derteufelqwe.driver;

import lombok.SneakyThrows;
import org.newsclub.net.unix.AFUNIXServerSocket;
import org.newsclub.net.unix.AFUNIXSocketAddress;

import java.io.File;
import java.util.concurrent.TimeUnit;

public class Main {

    @SneakyThrows
    public static void main(String[] args) {
        DMCLogDriver driver = null;

        try {
            driver = new DMCLogDriver();
            driver.addSignalHook();
            driver.startServer();

        } catch (Exception e) {
            System.err.println("Failed to start LogDriver. Error: " + e.getMessage());
            e.printStackTrace(System.err);
            driver.shutdown();
        }
    }

}
