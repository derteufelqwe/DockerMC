package de.derteufelqwe.bungeeplugin.docker;

import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import sun.misc.Signal;
import sun.misc.SignalHandler;

/**
 * Handles Dockers kill signals
 */
public class DockerSignalHandler implements SignalHandler {

    public static void listenTo(String name) {
        Signal signal = new Signal(name);
        Signal.handle(signal, new DockerSignalHandler());
    }

    @Override
    public void handle(Signal signal) {
        System.out.println("Received Signal " + signal);
        if (signal.equals(new Signal("TERM"))) {
            System.err.println("[CRITICAL WARNING] EXTERNALLY FORCED SHUTDOWN BY DOCKER!");

            for (ProxiedPlayer player : ProxyServer.getInstance().getPlayers()) {
                player.disconnect(new TextComponent(ChatColor.RED + "BungeeCord Proxy externally forced shutdown!"));
            }

            ProxyServer.getInstance().stop("Externally forced shutdown");
        }

        ProxyServer.getInstance().stop("Emergency shutdown.");
    }
}
