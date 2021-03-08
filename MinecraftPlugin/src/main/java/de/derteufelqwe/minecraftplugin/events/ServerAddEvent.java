package de.derteufelqwe.minecraftplugin.events;


import lombok.AllArgsConstructor;
import lombok.Getter;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

import java.net.Inet4Address;

/**
 * Fired when a server was started and added to BungeeCord
 */
@AllArgsConstructor
@Getter
public class ServerAddEvent extends Event {

    private static final HandlerList HANDLERS = new HandlerList();

    private String name;
    private String containerID;
    private Inet4Address ip;
    private String serviceId;


    public static HandlerList getHandlerList() {
        return HANDLERS;
    }

    @Override
    public HandlerList getHandlers() {
        return HANDLERS;
    }

}
