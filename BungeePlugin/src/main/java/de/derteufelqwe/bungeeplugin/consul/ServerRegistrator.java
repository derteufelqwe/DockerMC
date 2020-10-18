package de.derteufelqwe.bungeeplugin.consul;

import com.orbitz.consul.model.catalog.CatalogService;
import de.derteufelqwe.commons.consul.ICacheChangeListener;
import net.md_5.bungee.api.ProxyServer;

import java.net.InetSocketAddress;

public class ServerRegistrator implements ICacheChangeListener<String, CatalogService> {

    public ServerRegistrator() {

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


    /**
     * Called when a Server doesn't get deregistered on shutdown and gets registered again
     */
    @Override
    public void onModifyEntry(String key, CatalogService value) {
        String serverName = getServerName(key, value);
        System.out.println(String.format("[Warning] Modified %s to %s.", key, value));

        this.removeServer(serverName);
        this.addServer(serverName, value.getServiceAddress(), value.getServicePort());
    }

    @Override
    public void onAddEntry(String key, CatalogService value) {
        String serverName = getServerName(key, value);

        this.addServer(serverName, value.getServiceAddress(), value.getServicePort());
    }

    @Override
    public void onRemoveEntry(String key, CatalogService value) {
        String serverName = getServerName(key, value);

        this.removeServer(serverName);
    }


    private void addServer(String serverName, String address, int port) {
        ProxyServer.getInstance().getConfig().addServer(ProxyServer.getInstance().constructServerInfo(
                serverName, new InetSocketAddress(address, port),
                "Motd", false
        ));
        System.out.println("Added Server " + serverName + ".");
    }

    private void removeServer(String serverName) {
        ProxyServer.getInstance().getConfig().removeServerNamed(serverName);

        System.out.println("Removed Server " + serverName + ".");
    }

}
