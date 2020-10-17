package de.derteufelqwe.bungeeplugin.consul;

import com.orbitz.consul.CatalogClient;
import com.orbitz.consul.cache.ServiceCatalogCache;
import com.orbitz.consul.model.catalog.CatalogService;
import de.derteufelqwe.commons.consul.CacheListener;
import de.derteufelqwe.commons.consul.ICacheChangeListener;
import net.md_5.bungee.api.ProxyServer;

import java.net.InetSocketAddress;

public class MinecraftServiceListener {

    private CatalogClient catalogClient;
    private ServiceCatalogCache serviceCatalogCache;
    private CacheListener<String, CatalogService> cacheListener;


    public MinecraftServiceListener(CatalogClient catalogClient) {
        this.catalogClient = catalogClient;

        this.serviceCatalogCache = ServiceCatalogCache.newCache(this.catalogClient, "minecraft");
        this.cacheListener = new CacheListener<>();

        this.serviceCatalogCache.addListener(cacheListener);
    }

    public void addListener(ICacheChangeListener<String, CatalogService> listener) {
        this.cacheListener.addListener(listener);
    }


    public void start() {
        this.serviceCatalogCache.start();
    }

    public void stop() {
        this.serviceCatalogCache.stop();
    }

}
