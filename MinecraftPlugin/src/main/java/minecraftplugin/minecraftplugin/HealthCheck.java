package minecraftplugin.minecraftplugin;

import com.sun.net.httpserver.HttpServer;
import org.bukkit.Bukkit;

import java.io.IOException;
import java.net.InetSocketAddress;

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
            Bukkit.getServer().shutdown();
        }
    }

    public void stop() {
        server.stop(0);
        System.out.println("Stopped Healthcheck server.");
    }

}