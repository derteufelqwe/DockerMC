package de.derteufelqwe.bungeeplugin.utils;

import com.google.common.collect.HashBiMap;
import de.derteufelqwe.commons.Constants;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.config.ServerInfo;

import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;

public class Utils {

    /**
     * Creates the labels required for container identification.
     *
     * @return
     */
    public static Map<String, String> quickLabel(Constants.ContainerType containerType) {
        Map<String, String> labelsMap = new HashMap<>();
        labelsMap.put(Constants.DOCKER_IDENTIFIER_KEY, Constants.DOCKER_IDENTIFIER_VALUE);
        labelsMap.put(Constants.CONTAINER_IDENTIFIER_KEY, containerType.name());

        return labelsMap;
    }

    /**
     * Returns a copy of all available servers excluding the reserved servers, which have special purposes.
     */
    public static Map<String, ServerInfo> getServers() {
        Map<String, ServerInfo> serverMap = new HashMap<>();
        serverMap.putAll(ProxyServer.getInstance().getServersCopy());

        serverMap.remove("default");
        serverMap.remove("toLobby");

        return serverMap;
    }


}
