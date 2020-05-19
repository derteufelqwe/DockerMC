package de.derteufelqwe.bungeeplugin.consul;

import com.orbitz.consul.CatalogClient;
import com.orbitz.consul.cache.ServiceCatalogCache;
import com.orbitz.consul.model.catalog.CatalogService;
import net.md_5.bungee.api.ProxyServer;

import java.net.InetSocketAddress;

public class MinecraftKeyListener implements ICacheChangeListener<String, CatalogService> {

    private CatalogClient catalogClient;
    private ServiceCatalogCache serviceCatalogCache;


    public MinecraftKeyListener(CatalogClient catalogClient) {
        this.catalogClient = catalogClient;

        this.serviceCatalogCache = ServiceCatalogCache.newCache(this.catalogClient, "minecraft");
        CacheListener<String, CatalogService> cacheListener = new CacheListener<>();
        cacheListener.addListener(this);

        this.serviceCatalogCache.addListener(cacheListener);
        this.serviceCatalogCache.start();
    }

    /**
     * Generates the Servername based on the Container name (key) and the meta data from the Consul Service (value)
     *
     * @param key   Task name in docker and at the same time the ServiceID in Consul
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


    @Override
    public void onModifyEntry(String key, CatalogService value) {
        System.err.println(String.format("[Warning] Modified %s to %s.", key, value));
    }

    @Override
    public void onAddEntry(String key, CatalogService value) {
        String serverName = getServerName(key, value);

        ProxyServer.getInstance().getConfig().addServer(ProxyServer.getInstance().constructServerInfo(
                serverName, new InetSocketAddress(value.getServiceAddress(), value.getServicePort()),
                "Motd", false
        ));
        System.out.println("Added Server " + serverName + ".");
    }

    @Override
    public void onRemoveEntry(String key, CatalogService value) {
        String serverName = getServerName(key, value);

        ProxyServer.getInstance().getConfig().removeServerNamed(serverName);

        System.out.println("Removed Server " + serverName + ".");
    }


    public void stop() {
        this.serviceCatalogCache.stop();
    }

}
