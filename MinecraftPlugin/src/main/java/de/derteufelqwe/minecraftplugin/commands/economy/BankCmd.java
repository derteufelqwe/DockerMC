package de.derteufelqwe.minecraftplugin.commands.economy;

import co.aikar.commands.BaseCommand;
import co.aikar.commands.annotation.*;
import de.derteufelqwe.commons.CommonsAPI;
import de.derteufelqwe.commons.hibernate.SessionBuilder;
import de.derteufelqwe.commons.hibernate.objects.DBPlayer;
import de.derteufelqwe.commons.hibernate.objects.economy.Bank;
import de.derteufelqwe.commons.hibernate.objects.economy.BankTransaction;
import de.derteufelqwe.commons.hibernate.objects.economy.PlayerToBank;
import de.derteufelqwe.commons.misc.Pair;
import de.derteufelqwe.minecraftplugin.MinecraftPlugin;
import de.derteufelqwe.minecraftplugin.Utils;
import de.derteufelqwe.minecraftplugin.economy.DMCEconomy;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.chat.TextComponent;
import net.milkbowl.vault.economy.EconomyResponse;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.hibernate.Session;
import org.hibernate.Transaction;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@CommandAlias("bank")
public class BankCmd extends BaseCommand {

    private final int playersPerPage = 15;
    private final String PREFIX = ChatColor.GOLD + "[DMCBank] " + ChatColor.RESET;

    private DMCEconomy economy = MinecraftPlugin.getEconomy();
    private SessionBuilder sessionBuilder = MinecraftPlugin.getSessionBuilder();

    private Map<UUID, Pair<String, Long>> removeBankConfirmationMap = new HashMap<>();


    private void send(Player player, String msg, Object... args) {
        player.sendMessage(new TextComponent(String.format(msg, args)));
    }

    @Default
    @CatchUnknown
    @Subcommand("help")
    public void printHelp(Player player) {
        send(player, PREFIX + ChatColor.GOLD + "---- Bank help ----");
        send(player, "%s balance%s - %s Shows the balance on the bank account", ChatColor.YELLOW, ChatColor.RESET, ChatColor.GRAY);
        send(player, "%s deposit%s - %s Deposits money from the bank account", ChatColor.YELLOW, ChatColor.RESET, ChatColor.GRAY);
        send(player, "%s withdraw%s - %s Withdraws money from the bank account", ChatColor.YELLOW, ChatColor.RESET, ChatColor.GRAY);
        send(player, "%s transactions%s - %s Shows the transactions on the bank account.", ChatColor.YELLOW, ChatColor.RESET, ChatColor.GRAY);
        send(player, "%s list%s - %s Lists all banks you have access to", ChatColor.YELLOW, ChatColor.RESET, ChatColor.GRAY);
        send(player, "%s create%s - %s Creates a new bank", ChatColor.YELLOW, ChatColor.RESET, ChatColor.GRAY);
        send(player, "%s remove%s - %s Deletes a bank", ChatColor.YELLOW, ChatColor.RESET, ChatColor.GRAY);
        send(player, "%s setowner%s - %s Changes the ownership of a bank", ChatColor.YELLOW, ChatColor.RESET, ChatColor.GRAY);
        send(player, "%s members%s - %s Lists all members of a bank.", ChatColor.YELLOW, ChatColor.RESET, ChatColor.GRAY);
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

    @Subcommand("remove|delete")
    public void removeBank(Player player, String name, @Optional String confirmation) {
        EconomyResponse resp1 = economy.isBankOwner(name, player);
        if (resp1.type != EconomyResponse.ResponseType.SUCCESS) {
            if (resp1.amount == 0) {
                send(player, PREFIX + ChatColor.RED + "Bank named %s not found.", name);

            } else if (resp1.amount == 1) {
                send(player, PREFIX + ChatColor.RED + "You are not the owner of bank %s.", name);
            }

            return;
        }

        Pair<String, Long> removeConfirmation = this.removeBankConfirmationMap.get(player.getUniqueId());
        if (removeConfirmation == null || !removeConfirmation.getA().equals(name)
                || System.currentTimeMillis() > removeConfirmation.getB() + 20000 || confirmation == null || !confirmation.equals("confirm")) {

            send(player, PREFIX + ChatColor.RED + "Removing the bank %s can't be undone and all money will be gone.%s Append 'confirm' to your command to remove this bank forever.", name, ChatColor.RESET);
            this.removeBankConfirmationMap.put(player.getUniqueId(), new Pair<>(name, System.currentTimeMillis()));
            return;

        } else {
            this.removeBankConfirmationMap.remove(player.getUniqueId());
        }

        EconomyResponse resp2 = economy.deleteBank(name);

        if (resp2.type != EconomyResponse.ResponseType.SUCCESS) {
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

        Bukkit.getScheduler().runTaskAsynchronously(MinecraftPlugin.getINSTANCE(), () -> {
            try (Session session = sessionBuilder.openSession()) {
                Transaction tx = session.beginTransaction();

                DBPlayer dbPlayer = CommonsAPI.getInstance().getPlayerFromDB(session, player.getUniqueId());
                Bank bank = session.get(Bank.class, name);

                BankTransaction transaction = new BankTransaction(dbPlayer, bank, amount, true);
                session.persist(transaction);

                tx.commit();
            }
        });
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

        Bukkit.getScheduler().runTaskAsynchronously(MinecraftPlugin.getINSTANCE(), () -> {
            try (Session session = sessionBuilder.openSession()) {
                Transaction tx = session.beginTransaction();

                DBPlayer dbPlayer = CommonsAPI.getInstance().getPlayerFromDB(session, player.getUniqueId());
                Bank bank = session.get(Bank.class, name);

                BankTransaction transaction = new BankTransaction(dbPlayer, bank, amount, false);
                session.persist(transaction);

                tx.commit();
            }
        });
    }

    @Subcommand("transactions|trans")
    public void transactions(Player player, String bankName, @Default("1") int pageNumber) {
        try (Session session = sessionBuilder.openSession()) {
            Bank bank = session.get(Bank.class, bankName);
            if (bank == null) {
                send(player, PREFIX + ChatColor.RED + "Bank %s not found.", bankName);
                return;
            }

            if (economy.isBankMember(bankName, player).type != EconomyResponse.ResponseType.SUCCESS &&
                    economy.isBankOwner(bankName, player).type != EconomyResponse.ResponseType.SUCCESS) {
                send(player, PREFIX + ChatColor.RED + "You are no member of bank %s.", bankName);
                return;
            }

            Pair<Integer, Integer> slices = Utils.getPageSlices(pageNumber, playersPerPage, bank.getTransactions().size());

            send(player, PREFIX + ChatColor.GOLD + "---- Transactions (%s/%s) ----", pageNumber, (int) Math.ceil(bank.getTransactions().size() / (double) playersPerPage));
            for (BankTransaction trans : bank.getTransactions().subList(slices.getA(), slices.getB())) {
                String type = trans.isDeposit() ? ChatColor.GREEN + "D" : ChatColor.RED + "W";
                send(player, "%s%s: %s%s%s$%s (%s) - %s[%s]", type, ChatColor.RESET, ChatColor.GOLD, economy.format(trans.getAmount()),
                        ChatColor.GRAY, ChatColor.RESET, trans.getPlayer().getName(), ChatColor.YELLOW,
                        de.derteufelqwe.commons.Utils.formatTimestamp(trans.getTimestamp()));
            }
        }
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

    @Subcommand("setowner")
    public void setOwner(Player player, String bankName, String newOwnerName) {
        EconomyResponse resp1 = economy.isBankOwner(bankName, player);
        if (resp1.type != EconomyResponse.ResponseType.SUCCESS) {
            if (resp1.amount == 0) {
                send(player, PREFIX + ChatColor.RED + "Bank %s not found.", bankName);

            } else if (resp1.amount == 1) {
                send(player, PREFIX + ChatColor.RED + "You are not the owner of bank %s%s%s.", ChatColor.YELLOW, bankName, ChatColor.RESET);
            }

            return;
        }

        EconomyResponse resp2 = economy.changeBankOwner(bankName, newOwnerName);
        if (resp2.type != EconomyResponse.ResponseType.SUCCESS) {
            if (resp2.amount == 0) {
                send(player, PREFIX + ChatColor.RED + "Bank %s not found.", bankName);

            } else if (resp2.amount == 1) {
                send(player, PREFIX + ChatColor.RED + "Player %s%s%s not found.", ChatColor.YELLOW, newOwnerName, ChatColor.RED);
            }

            return;
        }

        send(player, PREFIX + "Changed owner of bank %s%s%s to %s%s%s.", ChatColor.GREEN, bankName, ChatColor.RESET, ChatColor.YELLOW, newOwnerName, ChatColor.RESET);
        economy.addBankMember(bankName, player.getName());
        economy.removeBankMember(bankName, newOwnerName);
    }

    @Subcommand("members")
    public void listMembers(Player player, String bankName, @Default("1") int pageNumber) {
        List<String> players = economy.getBankMembers(bankName);
        String owner = economy.getBankOwner(bankName);
        if (players == null || owner == null) {
            send(player, PREFIX + ChatColor.RED + "Bank %s not found.", bankName);
            return;
        }

        if (economy.isBankMember(bankName, player).type != EconomyResponse.ResponseType.SUCCESS &&
                economy.isBankOwner(bankName, player).type != EconomyResponse.ResponseType.SUCCESS) {
            send(player, PREFIX + ChatColor.RED + "You are no member of bank %s.", bankName);
            return;
        }

        Pair<Integer, Integer> slices = Utils.getPageSlices(pageNumber, playersPerPage, players.size());

        send(player, PREFIX + ChatColor.GOLD + "---- Bank members (%s/%s) ----", pageNumber, Math.ceil(players.size() / (double) playersPerPage));
        send(player, "%s %s(Owner)", owner, ChatColor.YELLOW);
        for (String playerName : players.subList(slices.getA(), slices.getB())) {
            send(player, playerName);
        }
    }

    @Subcommand("addmember")
    public void addMember(Player player, String bankName, String playerName) {
        // Check if you are the owner
        EconomyResponse resp1 = economy.isBankOwner(bankName, player);
        if (resp1.type == EconomyResponse.ResponseType.FAILURE) {
            if (resp1.amount == 0) {
                send(player, PREFIX + ChatColor.RED + "Bank %s not found.", bankName);

            } else if (resp1.amount == 1) {
                send(player, PREFIX + ChatColor.RED + "You are not the owner of bank %s%s%s.", ChatColor.YELLOW, bankName, ChatColor.RESET);
            }

            return;
        }

        // Add the member
        EconomyResponse resp2 = economy.addBankMember(bankName, playerName);

        if (resp2.type == EconomyResponse.ResponseType.FAILURE) {
            if (resp2.amount == 0) {
                send(player, PREFIX + ChatColor.RED + "Bank %s not found.", bankName);

            } else if (resp2.amount == 1) {
                send(player, PREFIX + ChatColor.RED + "Player %s not found.", playerName);

            } else if (resp2.amount == 2) {
                send(player, PREFIX + ChatColor.RED + "Player %s is the owner of the bank %s.", playerName, bankName);

            } else if (resp2.amount == 3) {
                send(player, PREFIX + ChatColor.RED + "Player %s is already a member of the bank %s.", playerName, bankName);
            }

            return;
        }

        send(player, PREFIX + "Added %s%s%s to %s%s%s.", ChatColor.YELLOW, playerName, ChatColor.RESET, ChatColor.GREEN, bankName, ChatColor.RESET);
    }

    @Subcommand("removemember|rmmember")
    public void removeMember(Player player, String bankName, String playerName) {
        // Check if you are the owner
        EconomyResponse resp1 = economy.isBankOwner(bankName, player);
        if (resp1.type == EconomyResponse.ResponseType.FAILURE) {
            if (resp1.amount == 0) {
                send(player, PREFIX + ChatColor.RED + "Bank %s not found.", bankName);

            } else if (resp1.amount == 1) {
                send(player, PREFIX + ChatColor.RED + "You are not the owner of bank %s%s%s.", ChatColor.YELLOW, bankName, ChatColor.RESET);
            }

            return;
        }

        // Add the member
        EconomyResponse resp2 = economy.removeBankMember(bankName, playerName);

        if (resp2.type == EconomyResponse.ResponseType.FAILURE) {
            if (resp2.amount == 0) {
                send(player, PREFIX + ChatColor.RED + "Bank %s not found.", bankName);

            } else if (resp2.amount == 1) {
                send(player, PREFIX + ChatColor.RED + "Player %s not found.", playerName);

            } else if (resp2.amount == 2) {
                send(player, PREFIX + ChatColor.RED + "Player %s is not a member of the bank %s.", playerName, bankName);
            }

            return;
        }

        send(player, PREFIX + "Removed %s%s%s from %s%s%s.", ChatColor.YELLOW, playerName, ChatColor.RESET, ChatColor.GREEN, bankName, ChatColor.RESET);
    }

}
