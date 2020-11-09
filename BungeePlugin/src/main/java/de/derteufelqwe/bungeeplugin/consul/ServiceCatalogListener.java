package de.derteufelqwe.bungeeplugin.consul;

import com.orbitz.consul.CatalogClient;
import com.orbitz.consul.cache.ServiceCatalogCache;
import com.orbitz.consul.model.catalog.CatalogService;
import de.derteufelqwe.commons.consul.ConsulCacheDistributor;

/**
 * This class can be treated like the consul ServiceCatalog with the exception, that only value changes get notified.
 * Programs probably only need one instance of this class and add as many listeners as they want.
 */
public class ServiceCatalogListener extends ConsulCacheDistributor<String, CatalogService> {

    private CatalogClient catalogClient;
    private String serviceName;
    private ServiceCatalogCache serviceCatalogCache;


    public ServiceCatalogListener(CatalogClient catalogClient, String serviceName) {
        super();

        this.catalogClient = catalogClient;
        this.serviceName = serviceName;
    }


    public void init() {
        this.serviceCatalogCache = ServiceCatalogCache.newCache(catalogClient, serviceName);
        this.serviceCatalogCache.addListener(this);
    }

    public void start() {
        this.serviceCatalogCache.start();
    }

    public void stop() {
        this.serviceCatalogCache.stop();
    }

}
