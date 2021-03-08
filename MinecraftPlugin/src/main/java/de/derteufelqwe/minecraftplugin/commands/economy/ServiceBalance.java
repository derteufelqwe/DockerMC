package de.derteufelqwe.minecraftplugin.commands.economy;

import co.aikar.commands.BaseCommand;
import co.aikar.commands.annotation.CommandAlias;
import co.aikar.commands.annotation.Default;
import co.aikar.commands.annotation.Optional;
import de.derteufelqwe.commons.hibernate.SessionBuilder;
import de.derteufelqwe.minecraftplugin.MinecraftPlugin;
import de.derteufelqwe.minecraftplugin.economy.DMCEconomy;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.entity.Player;

@CommandAlias("servicebalance|sbal")
public class ServiceBalance extends BaseCommand {

    private final String PREFIX = ChatColor.GOLD + "[DMCBal] " + ChatColor.RESET;

    private final DMCEconomy economy = MinecraftPlugin.getEconomy();
    private final SessionBuilder sessionBuilder = MinecraftPlugin.getSessionBuilder();
    private final String serviceName = MinecraftPlugin.getMetaData().getServerName();


    private void send(Player player, String msg, Object... args) {
        player.sendMessage(new TextComponent(String.format(msg, args)));
    }

    @Default
    public void showBalance(Player player, @Optional String serviceName, @Optional String playerName) {
        if (serviceName == null)
            serviceName = this.serviceName;

        double balance;
        if (playerName == null)
            balance = economy.getBalance(player, serviceName);
        else
            balance = economy.getBalance(playerName, serviceName);

        if (balance == -1) {
            send(player, PREFIX + "Player %s not found on %s.", playerName, serviceName);
            return;
        }

        send(player, "Your balance on %s%s%s: %s%s%s$", ChatColor.GREEN, serviceName, ChatColor.RESET, ChatColor.GOLD, balance, ChatColor.GRAY);
    }


}
