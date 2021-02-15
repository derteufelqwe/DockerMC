package minecraftplugin.minecraftplugin.commands.economy;

import co.aikar.commands.BaseCommand;
import co.aikar.commands.annotation.CommandAlias;
import co.aikar.commands.annotation.Default;
import de.derteufelqwe.commons.CommonsAPI;
import de.derteufelqwe.commons.hibernate.SessionBuilder;
import de.derteufelqwe.commons.hibernate.objects.DBPlayer;
import de.derteufelqwe.commons.hibernate.objects.economy.PlayerTransaction;
import minecraftplugin.minecraftplugin.MinecraftPlugin;
import minecraftplugin.minecraftplugin.economy.DMCEconomy;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.chat.TextComponent;
import net.milkbowl.vault.economy.EconomyResponse;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.hibernate.Session;
import org.hibernate.Transaction;

@CommandAlias("pay")
public class MainPay extends BaseCommand {

    private final String PREFIX = ChatColor.GOLD + "[DMCBal] " + ChatColor.RESET;

    private DMCEconomy economy = MinecraftPlugin.getEconomy();
    private SessionBuilder sessionBuilder = MinecraftPlugin.getSessionBuilder();


    private void send(Player player, String msg, Object... args) {
        player.sendMessage(new TextComponent(String.format(msg, args)));
    }

    @Default
    public void sendMoney(Player player, String receiver, double amount) {
        if (amount < 0) {
            send(player, PREFIX + "%sCan't send negative money.", ChatColor.RED);
            return;
        }

        if (!economy.hasAccount(receiver)) {
            send(player, PREFIX + "%sPlayer %s not found.", ChatColor.RED, receiver);
            return;
        }

        EconomyResponse resp1 = economy.withdrawPlayer(player, amount);
        if (resp1.type == EconomyResponse.ResponseType.FAILURE) {
            send(player, PREFIX + "%sYou don't have enough money.", ChatColor.RED);
            return;
        }

        EconomyResponse resp2 = economy.depositPlayer(receiver, amount);
        if (resp2.type == EconomyResponse.ResponseType.FAILURE) {
            send(player, PREFIX + "%sFailed to transfer the money.", ChatColor.RED);
            economy.depositPlayer(player, amount);
            return;
        }

        send(player, PREFIX + "Sent %s%s%s$ to %s.", ChatColor.GOLD, economy.format(amount), ChatColor.RESET, receiver);

        Bukkit.getScheduler().runTaskAsynchronously(MinecraftPlugin.getINSTANCE(), () -> {
            try (Session session = sessionBuilder.openSession()) {
                Transaction tx = session.beginTransaction();

                DBPlayer from = CommonsAPI.getInstance().getPlayerFromDB(session, player.getUniqueId());
                DBPlayer to = CommonsAPI.getInstance().getPlayerFromDB(session, receiver);

                PlayerTransaction transaction = new PlayerTransaction(from, to, amount);
                session.persist(transaction);

                tx.commit();
            }
        });
    }


}
