package minecraftplugin.minecraftplugin.commands.economy;

import co.aikar.commands.BaseCommand;
import co.aikar.commands.annotation.CommandAlias;
import co.aikar.commands.annotation.Default;
import co.aikar.commands.annotation.Optional;
import co.aikar.commands.annotation.Subcommand;
import de.derteufelqwe.commons.hibernate.SessionBuilder;
import minecraftplugin.minecraftplugin.MinecraftPlugin;
import minecraftplugin.minecraftplugin.economy.DMCEconomy;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.entity.Player;

@CommandAlias("mainbalance|mbal")
public class MainBalance extends BaseCommand {

    private final String PREFIX = ChatColor.GOLD + "[DMCBal] " + ChatColor.RESET;

    private DMCEconomy economy = MinecraftPlugin.getEconomy();
    private SessionBuilder sessionBuilder = MinecraftPlugin.getSessionBuilder();


    private void send(Player player, String msg, Object... args) {
        player.sendMessage(new TextComponent(String.format(msg, args)));
    }

    @Default
    public void showBalance(Player player, @Optional String playerName) {
        double balance;
        if (playerName == null)
            balance = economy.getBalance(player);
        else
            balance = economy.getBalance(playerName);

        if (balance == -1) {
            send(player, PREFIX + "Player %s not found.", playerName);
            return;
        }

        String balanceString = economy.format(balance);

        send(player, "Your balance: %s%s%s$", ChatColor.GOLD, balanceString, ChatColor.GRAY);
    }


}
