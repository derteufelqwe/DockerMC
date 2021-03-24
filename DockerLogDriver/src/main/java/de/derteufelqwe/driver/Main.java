package de.derteufelqwe.driver;

public class Main {

    public static void main(String[] args) {
        try {
            DMCLogDriver driver = new DMCLogDriver();
            driver.addSignalHook();
            driver.startServer();

        } catch (RuntimeException e) {
            e.printStackTrace(System.err);
            System.err.println("Failed to start LogDriver. Error: " + e.getMessage());
            System.exit(1);
        }
    }

}
