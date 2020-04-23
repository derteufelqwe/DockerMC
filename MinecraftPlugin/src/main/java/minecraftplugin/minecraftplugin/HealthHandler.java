package minecraftplugin.minecraftplugin;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.IOException;
import java.io.OutputStream;

public class HealthHandler implements HttpHandler {

    @Override
    public void handle(HttpExchange httpExchange) throws IOException {
        OutputStream outputStream = httpExchange.getResponseBody();

        httpExchange.sendResponseHeaders(200, 0);

        outputStream.write("".getBytes());
        outputStream.flush();
        outputStream.close();
    }
}
