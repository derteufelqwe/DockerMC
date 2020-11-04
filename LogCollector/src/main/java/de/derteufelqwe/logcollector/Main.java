package de.derteufelqwe.logcollector;

import lombok.SneakyThrows;

import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Main {


    @SneakyThrows
    public static void main(String[] args) {
        Logger.getLogger("org.hibernate").setLevel(Level.WARNING);

//        LogCollector logCollector = new LogCollector("tcp://ubuntu1:2375");
        LogCollector logCollector = new LogCollector("unix:///var/run/docker.sock");

        logCollector.start();

        System.out.println("Starting to listen for container deaths...");

        while (true) {
            TimeUnit.SECONDS.sleep(10);
        }
    }

}
