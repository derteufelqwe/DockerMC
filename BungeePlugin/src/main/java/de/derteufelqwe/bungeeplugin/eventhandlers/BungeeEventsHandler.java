package de.derteufelqwe.bungeeplugin.eventhandlers;

import de.derteufelqwe.bungeeplugin.events.BungeePlayerJoinEvent;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.event.EventHandler;

public class BungeeEventsHandler implements Listener {

    @EventHandler
    public void onJoin(BungeePlayerJoinEvent event) {
        System.out.println("Player " + event.getPlayerName() + " joined.");
    }

}
