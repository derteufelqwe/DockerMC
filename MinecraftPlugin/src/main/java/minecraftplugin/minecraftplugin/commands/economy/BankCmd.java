package minecraftplugin.minecraftplugin.commands.economy;

import co.aikar.commands.BaseCommand;
import co.aikar.commands.annotation.CatchUnknown;
import co.aikar.commands.annotation.CommandAlias;
import co.aikar.commands.annotation.Default;
import co.aikar.commands.annotation.Subcommand;
import de.derteufelqwe.commons.hibernate.SessionBuilder;
import de.derteufelqwe.commons.hibernate.objects.DBPlayer;
import de.derteufelqwe.commons.hibernate.objects.economy.Bank;
import de.derteufelqwe.commons.hibernate.objects.economy.PlayerToBank;
import minecraftplugin.minecraftplugin.MinecraftPlugin;
import minecraftplugin.minecraftplugin.economy.DMCEconomy;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.chat.TextComponent;
import net.milkbowl.vault.economy.EconomyResponse;
import org.bukkit.entity.Player;
import org.hibernate.Session;

@CommandAlias("bank")
public class BankCmd extends BaseCommand {

    private final String PREFIX = ChatColor.GOLD + "[DMCBank] " + ChatColor.RESET;

    private DMCEconomy economy = MinecraftPlugin.getEconomy();
    private SessionBuilder sessionBuilder = MinecraftPlugin.getSessionBuilder();


    private void send(Player player, String msg, Object... args) {
        player.sendMessage(new TextComponent(String.format(msg, args)));
    }

    @Default
    @CatchUnknown
    @Subcommand("help")
    public void printHelp(Player player) {
        send(player, PREFIX + ChatColor.GOLD + "---- Bank help ----");
        send(player, "%s balance%s - %s Shows the balance on the bank account", ChatColor.YELLOW, ChatColor.RESET, ChatColor.GRAY);
        send(player, "%s withdraw%s - %s Withdraws money from the bank account", ChatColor.YELLOW, ChatColor.RESET, ChatColor.GRAY);
        send(player, "%s deposit%s - %s Deposits money from the bank account", ChatColor.YELLOW, ChatColor.RESET, ChatColor.GRAY);
        send(player, "%s list%s - %s Lists all banks you have access to", ChatColor.YELLOW, ChatColor.RESET, ChatColor.GRAY);
        send(player, "%s create%s - %s Creates a new bank", ChatColor.YELLOW, ChatColor.RESET, ChatColor.GRAY);
        send(player, "%s remove%s - %s Deletes a bank", ChatColor.YELLOW, ChatColor.RESET, ChatColor.GRAY);
        send(player, "%s setownership%s - %s Changes the ownership of a bank", ChatColor.YELLOW, ChatColor.RESET, ChatColor.GRAY);
        send(player, "%s addmember%s - %s Adds a member to a bank account", ChatColor.YELLOW, ChatColor.RESET, ChatColor.GRAY);
        send(player, "%s removemember%s - %s Removes a member to a bank account", ChatColor.YELLOW, ChatColor.RESET, ChatColor.GRAY);
    }


    @Subcommand("create")
    public void createBank(Player player, String name) {
        EconomyResponse resp = economy.createBank(name, player);

        if (resp.type != EconomyResponse.ResponseType.SUCCESS) {
            send(player, PREFIX + ChatColor.RED + "Failed to create bank named %s. Name is already taken.", name);
            return;
        }

        send(player, PREFIX + "Created bank %s.", name);
    }

    @Subcommand("remove")
    public void removeBank(Player player, String name) {
        EconomyResponse resp = economy.deleteBank(name);

        if (resp.type != EconomyResponse.ResponseType.SUCCESS) {
            send(player, PREFIX + ChatColor.RED + "Failed to delete bank named %s.", name);
            return;
        }

        send(player, PREFIX + "Deleted bank %s.", name);
    }

    @Subcommand("balance|bal")
    public void bankBalance(Player player, String name) {
        EconomyResponse resp = economy.bankBalance(name);

        if (resp.type != EconomyResponse.ResponseType.SUCCESS) {
            send(player, PREFIX + ChatColor.RED + "Bank named %s not found.", name);
            return;
        }

        if (economy.isBankMember(name, player).type != EconomyResponse.ResponseType.SUCCESS &&
                economy.isBankOwner(name, player).type != EconomyResponse.ResponseType.SUCCESS) {
            send(player, PREFIX + ChatColor.RED + "You are no member of bank %s.", name);
            return;
        }

        String balance = economy.format(resp.balance);

        send(player, PREFIX + "Bank %s%s%s has %s%s%s$.", ChatColor.GREEN, name, ChatColor.RESET, ChatColor.GOLD, balance, ChatColor.RESET);
    }

    @Subcommand("deposit")
    public void deposit(Player player, String name, double amount) {
        if (amount < 0) {
            send(player, PREFIX + ChatColor.RED + "Can't deposit negative money.");
            return;
        }

        // Check if user is member or owner
        if (economy.isBankMember(name, player).type != EconomyResponse.ResponseType.SUCCESS &&
                economy.isBankOwner(name, player).type != EconomyResponse.ResponseType.SUCCESS) {
            send(player, PREFIX + ChatColor.RED + "You are no member of bank %s.", name);
            return;
        }

        if (!economy.has(player, amount)) {
            send(player, PREFIX + ChatColor.RED + "You don't have enough money.");
            return;
        }

        // Transfer money
        if (economy.withdrawPlayer(player, amount).type != EconomyResponse.ResponseType.SUCCESS) {
            send(player, PREFIX + ChatColor.RED + "Failed to withdraw you money.");
            return;
        }

        if (economy.bankDeposit(name, amount).type != EconomyResponse.ResponseType.SUCCESS) {
            send(player, PREFIX + ChatColor.RED + "Deposit failed", name);
            economy.depositPlayer(player, amount);
            return;
        }

        send(player, PREFIX + "Deposited %s%s%s$ to the bank %s.", ChatColor.GOLD, amount, ChatColor.RESET, name);
    }

    @Subcommand("withdraw")
    public void withdraw(Player player, String name, double amount) {
        if (amount < 0) {
            send(player, PREFIX + ChatColor.RED + "Can't withdraw negative money.");
            return;
        }

        if (economy.isBankMember(name, player).type != EconomyResponse.ResponseType.SUCCESS &&
                economy.isBankOwner(name, player).type != EconomyResponse.ResponseType.SUCCESS) {
            send(player, PREFIX + ChatColor.RED + "You are no member of bank %s.", name);
            return;
        }

        // Transfer the money
        EconomyResponse resp = economy.bankWithdraw(name, amount);
        if (resp.type != EconomyResponse.ResponseType.SUCCESS) {
            if (resp.balance == 0)
                send(player, PREFIX + ChatColor.RED + "Bank %s not found.", name);
            else if (resp.balance == -1)
                send(player, PREFIX + ChatColor.RED + "Bank %s has to little money.", name);

            return;
        }

        if (economy.depositPlayer(player, amount).type != EconomyResponse.ResponseType.SUCCESS) {
            economy.bankDeposit(name, amount);
            send(player, PREFIX + ChatColor.RED + "Failed to withdraw the money.", name);
        }

        send(player, PREFIX + "Withdrew %s%s%s$ from the bank %s.", ChatColor.GOLD, amount, ChatColor.RESET, name);
    }

    @Subcommand("list")
    public void listBanks(Player player) {
        try (Session session = sessionBuilder.openSession()) {
            DBPlayer dbPlayer = session.get(DBPlayer.class, player.getUniqueId());

            send(player, PREFIX + ChatColor.GOLD + "Your banks:");
            for (Bank bank : dbPlayer.getOwnedBanks()) {
                send(player, "%s - %s%s%s$ - %s(Owner)", bank.getName(), ChatColor.GOLD, bank.getMoneyBalance(), ChatColor.RESET, ChatColor.YELLOW);
            }

            for (PlayerToBank playerToBank : dbPlayer.getBanks()) {
                send(player, "%s - %s%s%s$", playerToBank.getBank().getName(), ChatColor.GOLD, playerToBank.getBank().getMoneyBalance(), ChatColor.RESET);
            }
        }
    }

}
