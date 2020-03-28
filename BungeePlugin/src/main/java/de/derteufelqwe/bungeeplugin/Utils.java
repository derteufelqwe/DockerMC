package de.derteufelqwe.bungeeplugin;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.config.ServerInfo;

import java.util.Collection;
import java.util.List;
import java.util.Map;

public class Utils {

    public static Map<String, ServerInfo> getServers() {
        Map<String, ServerInfo> serverMap = HashBiMap.create();
        serverMap.putAll(ProxyServer.getInstance().getServersCopy());

        serverMap.remove("default");

        return serverMap;
    }

}
