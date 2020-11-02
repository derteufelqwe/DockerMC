package de.derteufelqwe.ServerManager;

public class Main {

    public static void main(String[] args) throws Exception {
        ServerManager serverManager = new ServerManager();

        try {
            serverManager.start();




        } finally {
            serverManager.stop();
        }

    }

}
