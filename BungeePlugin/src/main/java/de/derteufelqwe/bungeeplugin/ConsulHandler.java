package de.derteufelqwe.bungeeplugin;

import com.orbitz.consul.AgentClient;
import com.orbitz.consul.CatalogClient;
import com.orbitz.consul.Consul;
import com.orbitz.consul.cache.ServiceCatalogCache;
import com.orbitz.google.common.net.HostAndPort;

public class ConsulHandler {

    private Consul consul;
    private AgentClient agentClient;
    private CatalogClient catalogClient;
    private ServiceCatalogCache serviceCatalogCache;


    public ConsulHandler() {
        this.consul = Consul.builder().withHostAndPort(HostAndPort.fromParts("consul_server", 8500)).build();
        this.agentClient = consul.agentClient();
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
