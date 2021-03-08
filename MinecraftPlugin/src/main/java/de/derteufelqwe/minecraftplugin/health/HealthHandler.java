package de.derteufelqwe.minecraftplugin.health;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.IOException;
import java.io.OutputStream;

public class HealthHandler implements HttpHandler {

    public static boolean healthy = true;

    @Override
    public void handle(HttpExchange httpExchange) throws IOException {
        OutputStream outputStream = httpExchange.getResponseBody();

        if (healthy) {
            httpExchange.sendResponseHeaders(200, 0);

        } else {
            httpExchange.sendResponseHeaders(500, 0);
        }

        outputStream.write("".getBytes());
        outputStream.flush();
        outputStream.close();
    }
}
