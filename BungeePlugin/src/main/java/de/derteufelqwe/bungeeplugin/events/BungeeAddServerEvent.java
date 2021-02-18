package de.derteufelqwe.bungeeplugin.events;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import net.md_5.bungee.api.Callback;
import net.md_5.bungee.api.event.AsyncEvent;

import java.net.Inet4Address;
import java.util.UUID;

/**
 * Fired when a new Minecraft server is ready and wants to be registered to BungeeCord
 */
@Getter
@ToString
@EqualsAndHashCode
public class BungeeAddServerEvent extends AsyncEvent<BungeeAddServerEvent> {

    private String name;
    private Inet4Address ip;
    private String containerId;
    private String serviceId;


    public BungeeAddServerEvent(String name, Inet4Address ip, String containerId, String serviceId, Callback<BungeeAddServerEvent> done) {
        super(done);
        this.name = name;
        this.ip = ip;
        this.containerId = containerId;
        this.serviceId = serviceId;
    }

}
