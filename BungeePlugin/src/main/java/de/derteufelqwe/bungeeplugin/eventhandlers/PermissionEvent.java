package de.derteufelqwe.bungeeplugin.eventhandlers;

import net.md_5.bungee.api.event.PermissionCheckEvent;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.event.EventHandler;

/**
 * Only listens for the PermissionCheckEvent event
 */
public class PermissionEvent implements Listener {


    @EventHandler
    public void onCheckPermission(PermissionCheckEvent event) {
        event.setHasPermission(true);
    }

}
