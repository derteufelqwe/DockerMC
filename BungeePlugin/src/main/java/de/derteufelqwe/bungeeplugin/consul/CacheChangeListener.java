package de.derteufelqwe.bungeeplugin.consul;

import com.orbitz.consul.cache.ConsulCache;
import com.orbitz.consul.model.catalog.CatalogService;
import net.md_5.bungee.api.ProxyServer;

import java.net.InetSocketAddress;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class CacheChangeListener implements ConsulCache.Listener<String, CatalogService> {

    private Map<String, CatalogService> serviceMap = new HashMap<>();   // All currently available Services

    @Override
    public void notify(Map<String, CatalogService> newValues) {

        // Add Services
        for (String key : newValues.keySet()) {
            if (!this.serviceMap.containsKey(key)) {
                this.serviceMap.put(key, newValues.get(key));
                this.addServer(key, newValues.get(key));
            }
        }

        // Remove Services
        Set<String> keys = new HashSet<>(this.serviceMap.keySet());
        keys.removeAll(newValues.keySet());
        for (String key : keys) {
            this.removeServer(key, this.serviceMap.get(key));
            this.serviceMap.remove(key);
        }

    }

    /**
     * Generates the Servername based on the Container name (key) and the meta data from the Consul Service (value)
     * @param key Task name in docker and at the same time the ServiceID in Consul
     * @param value Consul service
     * @return The Servername
     */
    private String getServerName(String key, CatalogService value) {
        String serverName = value.getServiceMeta().get("serverName");
        if (serverName == null || serverName.equals("")) {
            serverName = "NO_NAME";
        }

        String serverNumber = null;
        if (key.split("\\.").length > 2) {
            serverNumber = key.split("\\.")[1];

        } else {
            serverNumber = key;
        }

        return serverName + "-" + serverNumber;
    }

    private void addServer(String key, CatalogService value) {
        String serverName = getServerName(key, value);

        ProxyServer.getInstance().getConfig().addServer(ProxyServer.getInstance().constructServerInfo(
                serverName, new InetSocketAddress(value.getServiceAddress(), value.getServicePort()),
                "Motd", false
        ));
        System.out.println("Added Server " + serverName + ".");
    }

    private void removeServer(String key, CatalogService value) {
        String serverName = getServerName(key, value);

        ProxyServer.getInstance().getConfig().removeServerNamed(serverName);

        System.out.println("Removed Server " + serverName + ".");
    }

}
