package de.derteufelqwe.bungeeplugin.health;

import com.sun.net.httpserver.HttpServer;
import net.md_5.bungee.api.ProxyServer;

import java.io.IOException;
import java.net.InetSocketAddress;

/**
 * Starts a little HTTP server
 */
public class HealthCheck {

    private HttpServer server;

    public void start() {
        try {
            server = HttpServer.create(new InetSocketAddress("0.0.0.0", 8001), 0);
            server.createContext("/health", new HealthHandler());
            server.start();

            System.out.println("Started Healthcheck server.");

        } catch (IOException e) {
            e.printStackTrace();
            System.err.println("[Fatal Error] Failed to start health check server.");
            ProxyServer.getInstance().stop("[Fatal Error] Failed to start health check server.");
        }
    }

    public void stop() {
        server.stop(0);
        System.out.println("Stopped Healthcheck server.");
    }

}
