package minecraftplugin.minecraftplugin.eventhandlers;

import de.derteufelqwe.commons.hibernate.SessionBuilder;
import minecraftplugin.minecraftplugin.MinecraftPlugin;
import minecraftplugin.minecraftplugin.economy.DMCEconomy;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

public class MiscEventHandler implements Listener {

    private final SessionBuilder sessionBuilder = MinecraftPlugin.getSessionBuilder();
    private final DMCEconomy economy = MinecraftPlugin.getEconomy();
    private final String serviceName = MinecraftPlugin.getMetaData().getServerName();


    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        // Create the service balance on join
        if (!economy.hasAccount(event.getPlayer(), serviceName)) {
            economy.createPlayerAccount(event.getPlayer(), serviceName);
        }
    }

}
