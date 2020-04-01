package de.derteufelqwe.bungeeplugin;

import com.google.common.collect.HashBiMap;
import de.derteufelqwe.commons.Constants;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.config.ServerInfo;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;
import sun.net.www.protocol.http.HttpURLConnection;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URL;
import java.util.Map;

public class Utils {

    public static Map<String, ServerInfo> getServers() {
        Map<String, ServerInfo> serverMap = HashBiMap.create();
        serverMap.putAll(ProxyServer.getInstance().getServersCopy());

        serverMap.remove("default");

        return serverMap;
    }

    public static Map<String, Object> requestConfigFile(Constants.Configs config) {
        Yaml yaml = new Yaml();
        String url = String.format("http://%s:%s/get?file=%s", Constants.WEBSERVER_CONTAINER_NAME, Constants.WEBSERVER_PORT, config.filename());

        try {
            URL url1 = new URL(url);
            HttpURLConnection connection = (HttpURLConnection) url1.openConnection();
            connection.setRequestMethod("GET");
            connection.connect();

            String page = "";
            BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()));
            String working;

            while ((working = in.readLine()) != null) {
                page += working + "\n";
            }

            return (Map<String, Object>) yaml.loadAs(page, Map.class);

        } catch (MalformedURLException e1) {

        } catch (ProtocolException e) {
            e.printStackTrace();

        } catch (IOException e) {
            e.printStackTrace();
        }

        return null;
    }

}
