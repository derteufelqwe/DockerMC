package de.derteufelqwe.bungeeplugin.events;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import net.md_5.bungee.api.Callback;
import net.md_5.bungee.api.event.AsyncEvent;

import java.net.Inet4Address;

/**
 * Fired when a new Minecraft server is ready and wants to be registered to BungeeCord
 */
@Getter
@ToString
@EqualsAndHashCode
public class BungeeAddServerEvent extends AsyncEvent<BungeeAddServerEvent> {

    private String servername;
    private Inet4Address ip;
    private String containerId;
    private String serviceId;


    public BungeeAddServerEvent(String serverName, Inet4Address ip, String containerId, String serviceId, Callback<BungeeAddServerEvent> done) {
        super(done);
        this.servername = serverName;
        this.ip = ip;
        this.containerId = containerId;
        this.serviceId = serviceId;
    }

}
