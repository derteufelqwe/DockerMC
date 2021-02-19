package de.derteufelqwe.bungeeplugin.utils;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.HashMap;
import java.util.Map;

/**
 * Stores additional data of a Minecraft server, which might be useful later on
 */
public class ServerInfoStorage {

    private Map<String, Infos> storage = new HashMap<>();


    public void set(String serverName, Infos infos) {
        this.storage.put(serverName, infos);
    }

    public void remove(String serverName) {
        this.storage.remove(serverName);
    }

    public Infos get(String serverName) {
        return this.storage.get(serverName);
    }


    @Data
    @AllArgsConstructor
    public static class Infos {
        private String containerId;
        private String serviceId;
    }

}
