package de.derteufelqwe.bungeeplugin.consul;

import com.orbitz.consul.CatalogClient;
import com.orbitz.consul.Consul;
import com.orbitz.consul.cache.ServiceCatalogCache;

public class ConsulHandler {

    private CatalogClient catalogClient;
    private ServiceCatalogCache serviceCatalogCache;


    public ConsulHandler(Consul consul) {
        this.catalogClient = consul.catalogClient();
    }


    public void startListener() {
        this.serviceCatalogCache = ServiceCatalogCache.newCache(this.catalogClient, "minecraft");
        this.serviceCatalogCache.addListener(new CacheChangeListener());
        this.serviceCatalogCache.start();
    }

    public void stopListener() {
        this.serviceCatalogCache.stop();
    }

}
