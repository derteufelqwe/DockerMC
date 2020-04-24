package de.derteufelqwe.bungeeplugin;

import com.orbitz.consul.cache.ConsulCache;
import com.orbitz.consul.model.catalog.CatalogService;
import net.md_5.bungee.api.ProxyServer;

import java.net.InetSocketAddress;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class CacheChangeListener implements ConsulCache.Listener<String, CatalogService> {

    private Map<String, CatalogService> serviceMap = new HashMap<>();

    @Override
    public void notify(Map<String, CatalogService> newValues) {

        for (String key : newValues.keySet()) {
            if (!this.serviceMap.containsKey(key)) {
                this.serviceMap.put(key, newValues.get(key));
                this.addServer(key, newValues.get(key));
            }
        }

        Set<String> keys = new HashSet<>(this.serviceMap.keySet());
        keys.removeAll(newValues.keySet());
        for (String key : keys) {
            this.removeServer(key, this.serviceMap.get(key));
            this.serviceMap.remove(key);
        }

        return;
    }

    public void addServer(String key, CatalogService value) {
        System.out.println("key: " + key);
        String serverName = value.getServiceMeta().get("serverName") + "-" + key.split("\\.")[1];

        ProxyServer.getInstance().getConfig().addServer(ProxyServer.getInstance().constructServerInfo(
                serverName, new InetSocketAddress(value.getServiceAddress(), value.getServicePort()),
                "Motd", false
        ));
        System.out.println("Added Server " + key);
    }

    public void removeServer(String key, CatalogService value) {
        String serverName = value.getServiceMeta().get("serverName") + "-" + key.split("\\.")[1];
        ProxyServer.getInstance().getConfig().removeServerNamed(serverName);

        System.out.println("Removed Server " + key);
    }

}
