package de.derteufelqwe.minecraftplugin.economy;

import de.derteufelqwe.commons.CommonsAPI;
import de.derteufelqwe.commons.hibernate.SessionBuilder;
import de.derteufelqwe.commons.hibernate.objects.DBPlayer;
import de.derteufelqwe.commons.hibernate.objects.DBService;
import de.derteufelqwe.commons.hibernate.objects.economy.Bank;
import de.derteufelqwe.commons.hibernate.objects.economy.PlayerToBank;
import de.derteufelqwe.commons.hibernate.objects.economy.ServiceBalance;
import de.derteufelqwe.minecraftplugin.DBQueries;
import de.derteufelqwe.minecraftplugin.MinecraftPlugin;
import net.milkbowl.vault.economy.EconomyResponse;
import org.bukkit.OfflinePlayer;
import org.hibernate.Session;
import org.hibernate.Transaction;

import javax.annotation.CheckForNull;
import javax.persistence.PersistenceException;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class DMCEconomyImpl implements DMCEconomy {

    private final SessionBuilder sessionBuilder = MinecraftPlugin.getSessionBuilder();


    public DMCEconomyImpl() {

    }


    @Override
    public boolean isEnabled() {
        return true;
    }

    @Override
    public String getName() {
        return "DMCMoney";
    }

    @Override
    public boolean hasBankSupport() {
        return true;
    }

    @Override
    public int fractionalDigits() {
        return 2;
    }

    @Override
    public String format(double amount) {
        return Double.toString(
                ((int) (amount * Math.pow(10, this.fractionalDigits()))) / Math.pow(10, this.fractionalDigits())
        );
    }

    @Override
    public String currencyNamePlural() {
        return "Dollars";
    }

    @Override
    public String currencyNameSingular() {
        return "Dollar";
    }



    @Override
    public boolean hasAccount(String playerName) {
        try (Session session = sessionBuilder.openSession()) {
            DBPlayer dbPlayer = CommonsAPI.getInstance().getPlayerFromDB(session, playerName);

            return dbPlayer != null;
        }
    }

    @Override
    public boolean hasAccount(OfflinePlayer player) {
        try (Session session = sessionBuilder.openSession()) {
            DBPlayer dbPlayer = CommonsAPI.getInstance().getPlayerFromDB(session, player.getUniqueId());

            return dbPlayer != null;
        }
    }

    @Override
    public boolean hasAccount(String playerName, String serviceName) {
        try (Session session = sessionBuilder.openSession()) {
            DBPlayer dbPlayer = CommonsAPI.getInstance().getPlayerFromDB(session, playerName);
            if (dbPlayer == null)
                return false;

            DBService dbService = CommonsAPI.getInstance().getActiveServiceFromDB(session, serviceName);
            if (dbService == null)
                return false;

            return CommonsAPI.getInstance().getPlayerBalanceOnService(session, dbPlayer, dbService) != null;
        }
    }

    @Override
    public boolean hasAccount(OfflinePlayer player, String serviceName) {
        try (Session session = sessionBuilder.openSession()) {
            DBPlayer dbPlayer = CommonsAPI.getInstance().getPlayerFromDB(session, player.getUniqueId());
            if (dbPlayer == null)
                return false;

            DBService dbService = CommonsAPI.getInstance().getActiveServiceFromDB(session, serviceName);
            if (dbService == null)
                return false;

            return CommonsAPI.getInstance().getPlayerBalanceOnService(session, dbPlayer, dbService) != null;
        }
    }

    // --- Get balance ---

    @Override
    public double getBalance(String playerName) {
        try (Session session = sessionBuilder.openSession()) {
            DBPlayer dbPlayer = CommonsAPI.getInstance().getPlayerFromDB(session, playerName);
            if (dbPlayer == null)
                return -1;

            return dbPlayer.getMoneyBalance();
        }
    }

    @Override
    public double getBalance(OfflinePlayer player) {
        try (Session session = sessionBuilder.openSession()) {
            DBPlayer dbPlayer = CommonsAPI.getInstance().getPlayerFromDB(session, player.getUniqueId());
            if (dbPlayer == null)
                return -1;

            return dbPlayer.getMoneyBalance();
        }
    }

    @Override
    public double getBalance(String playerName, String serviceName) {
        try (Session session = sessionBuilder.openSession()) {
            DBPlayer dbPlayer = CommonsAPI.getInstance().getPlayerFromDB(session, playerName);
            if (dbPlayer == null)
                return -1;

            DBService dbService = CommonsAPI.getInstance().getActiveServiceFromDB(session, serviceName);
            if (dbService == null)
                return -2;

            ServiceBalance serviceBalance = CommonsAPI.getInstance().getPlayerBalanceOnService(session, dbPlayer, dbService);
            if (serviceBalance == null)
                return -3;

            return serviceBalance.getMoneyBalance();
        }
    }

    @Override
    public double getBalance(OfflinePlayer player, String serviceName) {
        try (Session session = sessionBuilder.openSession()) {
            DBPlayer dbPlayer = CommonsAPI.getInstance().getPlayerFromDB(session, player.getUniqueId());
            if (dbPlayer == null)
                return -1;

            DBService dbService = CommonsAPI.getInstance().getActiveServiceFromDB(session, serviceName);
            if (dbService == null)
                return -2;

            ServiceBalance serviceBalance = CommonsAPI.getInstance().getPlayerBalanceOnService(session, dbPlayer, dbService);
            if (serviceBalance == null)
                return -3;

            return serviceBalance.getMoneyBalance();
        }
    }

    // --- Has money ---

    @Override
    public boolean has(String playerName, double amount) {
        return getBalance(playerName) >= amount;
    }

    @Override
    public boolean has(OfflinePlayer player, double amount) {
        return getBalance(player) >= amount;
    }

    @Override
    public boolean has(String playerName, String serviceName, double amount) {
        return getBalance(playerName, serviceName) >= amount;
    }

    @Override
    public boolean has(OfflinePlayer player, String serviceName, double amount) {
        return getBalance(player, serviceName) >= amount;
    }

    // --- Withdraw ---

    private EconomyResponse withdrawPlayer(Session session, DBPlayer dbPlayer, double amount) {
        if (amount < 0)
            return new EconomyResponse(0, -1, EconomyResponse.ResponseType.FAILURE, "Negative balance.");

        if (dbPlayer.getMoneyBalance() < amount)
            return new EconomyResponse(0, dbPlayer.getMoneyBalance(), EconomyResponse.ResponseType.FAILURE, "Insufficient balance.");

        Transaction tx = session.beginTransaction();
        dbPlayer.setMoneyBalance(dbPlayer.getMoneyBalance() - amount);
        session.update(dbPlayer);
        tx.commit();

        return new EconomyResponse(amount, dbPlayer.getMoneyBalance(), EconomyResponse.ResponseType.SUCCESS, "Withdraw successful.");
    }

    private EconomyResponse withdrawPlayer(Session session, DBPlayer dbPlayer, String serviceName, double amount) {
        if (amount < 0)
            return new EconomyResponse(0, -1, EconomyResponse.ResponseType.FAILURE, "Negative balance.");

        DBService dbService = CommonsAPI.getInstance().getActiveServiceFromDB(session, serviceName);
        if (dbService == null)
            return new EconomyResponse(0, -1, EconomyResponse.ResponseType.FAILURE, "Service not found.");

        ServiceBalance serviceBalance = CommonsAPI.getInstance().getPlayerBalanceOnService(session, dbPlayer, dbService);
        if (serviceBalance == null)
            return new EconomyResponse(0, -1, EconomyResponse.ResponseType.FAILURE, "ServiceBalance not found.");

        if (serviceBalance.getMoneyBalance() < amount)
            return new EconomyResponse(0, dbPlayer.getMoneyBalance(), EconomyResponse.ResponseType.FAILURE, "Insufficient balance.");

        Transaction tx = session.beginTransaction();
        serviceBalance.setMoneyBalance(serviceBalance.getMoneyBalance() - amount);
        session.update(serviceBalance);
        tx.commit();

        return new EconomyResponse(amount, serviceBalance.getMoneyBalance(), EconomyResponse.ResponseType.SUCCESS, "Withdraw successful.");
    }

    @Override
    public EconomyResponse withdrawPlayer(String playerName, double amount) {
        try (Session session = sessionBuilder.openSession()) {
            DBPlayer dbPlayer = CommonsAPI.getInstance().getPlayerFromDB(session, playerName);
            if (dbPlayer == null)
                return new EconomyResponse(0, -1, EconomyResponse.ResponseType.FAILURE, "Player not found.");

            return withdrawPlayer(session, dbPlayer, amount);
        }
    }

    @Override
    public EconomyResponse withdrawPlayer(OfflinePlayer player, double amount) {
        try (Session session = sessionBuilder.openSession()) {
            DBPlayer dbPlayer = CommonsAPI.getInstance().getPlayerFromDB(session, player.getUniqueId());
            if (dbPlayer == null)
                return new EconomyResponse(0, -1, EconomyResponse.ResponseType.FAILURE, "Player not found.");

            return withdrawPlayer(session, dbPlayer, amount);
        }
    }

    @Override
    public EconomyResponse withdrawPlayer(String playerName, String serviceName, double amount) {
        try (Session session = sessionBuilder.openSession()) {
            DBPlayer dbPlayer = CommonsAPI.getInstance().getPlayerFromDB(session, playerName);
            if (dbPlayer == null)
                return new EconomyResponse(0, -1, EconomyResponse.ResponseType.FAILURE, "Player not found.");

            return withdrawPlayer(session, dbPlayer, serviceName, amount);
        }
    }

    @Override
    public EconomyResponse withdrawPlayer(OfflinePlayer player, String serviceName, double amount) {
        try (Session session = sessionBuilder.openSession()) {
            DBPlayer dbPlayer = CommonsAPI.getInstance().getPlayerFromDB(session, player.getUniqueId());
            if (dbPlayer == null)
                return new EconomyResponse(0, -1, EconomyResponse.ResponseType.FAILURE, "Player not found.");

            return withdrawPlayer(session, dbPlayer, serviceName, amount);
        }
    }

    // --- Deposit ---

    private EconomyResponse depositPlayer(Session session, DBPlayer dbPlayer, double amount) {
        if (amount < 0)
            return new EconomyResponse(0, -1, EconomyResponse.ResponseType.FAILURE, "Negative transfer value.");

        Transaction tx = session.beginTransaction();
        dbPlayer.setMoneyBalance(dbPlayer.getMoneyBalance() + amount);
        session.update(dbPlayer);
        tx.commit();

        return new EconomyResponse(amount, dbPlayer.getMoneyBalance(), EconomyResponse.ResponseType.SUCCESS, "Deposit successful.");
    }

    private EconomyResponse depositPlayer(Session session, DBPlayer dbPlayer, String serviceName, double amount) {
        if (amount < 0)
            return new EconomyResponse(0, -1, EconomyResponse.ResponseType.FAILURE, "Negative balance.");

        DBService dbService = CommonsAPI.getInstance().getActiveServiceFromDB(session, serviceName);
        if (dbService == null)
            return new EconomyResponse(0, -1, EconomyResponse.ResponseType.FAILURE, "Service not found.");

        ServiceBalance serviceBalance = CommonsAPI.getInstance().getPlayerBalanceOnService(session, dbPlayer, dbService);
        if (serviceBalance == null)
            return new EconomyResponse(0, -1, EconomyResponse.ResponseType.FAILURE, "ServiceBalance not found.");

        Transaction tx = session.beginTransaction();
        serviceBalance.setMoneyBalance(serviceBalance.getMoneyBalance() + amount);
        session.update(serviceBalance);
        tx.commit();

        return new EconomyResponse(amount, serviceBalance.getMoneyBalance(), EconomyResponse.ResponseType.SUCCESS, "Deposit successful.");
    }

    @Override
    public EconomyResponse depositPlayer(String playerName, double amount) {
        try (Session session = sessionBuilder.openSession()) {
            DBPlayer dbPlayer = CommonsAPI.getInstance().getPlayerFromDB(session, playerName);
            if (dbPlayer == null)
                return new EconomyResponse(0, -1, EconomyResponse.ResponseType.FAILURE, "Player not found.");

            return depositPlayer(session, dbPlayer, amount);
        }
    }

    @Override
    public EconomyResponse depositPlayer(OfflinePlayer player, double amount) {
        try (Session session = sessionBuilder.openSession()) {
            DBPlayer dbPlayer = CommonsAPI.getInstance().getPlayerFromDB(session, player.getUniqueId());
            if (dbPlayer == null)
                return new EconomyResponse(0, -1, EconomyResponse.ResponseType.FAILURE, "Player not found.");

            return depositPlayer(session, dbPlayer, amount);
        }
    }

    @Override
    public EconomyResponse depositPlayer(String playerName, String serviceName, double amount) {
        try (Session session = sessionBuilder.openSession()) {
            DBPlayer dbPlayer = CommonsAPI.getInstance().getPlayerFromDB(session, playerName);
            if (dbPlayer == null)
                return new EconomyResponse(0, -1, EconomyResponse.ResponseType.FAILURE, "Player not found.");

            return depositPlayer(session, dbPlayer, serviceName, amount);
        }
    }

    @Override
    public EconomyResponse depositPlayer(OfflinePlayer player, String serviceName, double amount) {
        try (Session session = sessionBuilder.openSession()) {
            DBPlayer dbPlayer = CommonsAPI.getInstance().getPlayerFromDB(session, player.getUniqueId());
            if (dbPlayer == null)
                return new EconomyResponse(0, -1, EconomyResponse.ResponseType.FAILURE, "Player not found.");

            return depositPlayer(session, dbPlayer, serviceName, amount);
        }
    }


    // --- Create bank ---

    private EconomyResponse createBank(Session session, String name, DBPlayer dbPlayer) {
        Transaction tx = session.beginTransaction();

        try {
            Bank bank = new Bank(name, dbPlayer);
            session.persist(bank);

            tx.commit();

        } catch (PersistenceException e) {
            tx.rollback();
            return new EconomyResponse(0, 0, EconomyResponse.ResponseType.FAILURE, "Failed to create bank.");
        }

        return new EconomyResponse(0, 0, EconomyResponse.ResponseType.SUCCESS, "Created bank successfully.");
    }

    @Override
    public EconomyResponse createBank(String name, String playerName) {
        try (Session session = sessionBuilder.openSession()) {
            DBPlayer dbPlayer = CommonsAPI.getInstance().getPlayerFromDB(session, playerName);
            if (dbPlayer == null)
                return new EconomyResponse(0, -1, EconomyResponse.ResponseType.FAILURE, "Player not found.");

            return createBank(session, name, dbPlayer);
        }
    }

    @Override
    public EconomyResponse createBank(String name, OfflinePlayer player) {
        try (Session session = sessionBuilder.openSession()) {
            DBPlayer dbPlayer = CommonsAPI.getInstance().getPlayerFromDB(session, player.getUniqueId());
            if (dbPlayer == null)
                return new EconomyResponse(0, -1, EconomyResponse.ResponseType.FAILURE, "Player not found.");

            return createBank(session, name, dbPlayer);
        }
    }

    @Override
    public EconomyResponse deleteBank(String name) {
        try (Session session = sessionBuilder.openSession()){
            Bank bank = session.get(Bank.class, name);
            if (bank == null)
                return new EconomyResponse(0, 0, EconomyResponse.ResponseType.FAILURE, "Bank not found.");

            Transaction tx = session.beginTransaction();
            session.delete(bank);
            tx.commit();

            return new EconomyResponse(0, 0, EconomyResponse.ResponseType.SUCCESS, "Deleted bank successfully.");
        }
    }

    @Override
    public EconomyResponse bankBalance(String name) {
        try (Session session = sessionBuilder.openSession()){
            Bank bank = session.get(Bank.class, name);
            if (bank == null)
                return new EconomyResponse(0, 0, EconomyResponse.ResponseType.FAILURE, "Bank not found.");

            return new EconomyResponse(bank.getMoneyBalance(), bank.getMoneyBalance(), EconomyResponse.ResponseType.SUCCESS, "Success.");
        }
    }


    @Override
    public EconomyResponse bankHas(String name, double amount) {
        try (Session session = sessionBuilder.openSession()){
            Bank bank = session.get(Bank.class, name);
            if (bank == null)
                return new EconomyResponse(0, 0, EconomyResponse.ResponseType.FAILURE, "Bank not found.");

            if (bank.getMoneyBalance() >= amount)
                return new EconomyResponse(amount, bank.getMoneyBalance(), EconomyResponse.ResponseType.SUCCESS, "Success.");
            else
                return new EconomyResponse(amount, bank.getMoneyBalance(), EconomyResponse.ResponseType.FAILURE, "Not enough money.");
        }
    }

    @Override
    public EconomyResponse bankWithdraw(String name, double amount) {
        try (Session session = sessionBuilder.openSession()){
            Bank bank = session.get(Bank.class, name);
            if (bank == null)
                return new EconomyResponse(0, 0, EconomyResponse.ResponseType.FAILURE, "Bank not found.");

            if (bank.getMoneyBalance() < amount)
                return new EconomyResponse(0, -1, EconomyResponse.ResponseType.FAILURE, "Too little money.");

            Transaction tx = session.beginTransaction();
            bank.setMoneyBalance(bank.getMoneyBalance() - amount);
            tx.commit();

            return new EconomyResponse(amount, bank.getMoneyBalance(), EconomyResponse.ResponseType.SUCCESS, "Success.");
        }
    }

    @Override
    public EconomyResponse bankDeposit(String name, double amount) {
        try (Session session = sessionBuilder.openSession()){
            Bank bank = session.get(Bank.class, name);
            if (bank == null)
                return new EconomyResponse(0, 0, EconomyResponse.ResponseType.FAILURE, "Bank not found.");

            Transaction tx = session.beginTransaction();
            bank.setMoneyBalance(bank.getMoneyBalance() + amount);
            tx.commit();

            return new EconomyResponse(amount, bank.getMoneyBalance(), EconomyResponse.ResponseType.SUCCESS, "Success.");
        }
    }


    private EconomyResponse isBankOwner(Session session, DBPlayer dbPlayer, String bankName) {
        Bank bank = session.get(Bank.class, bankName);
        if (bank == null)
            return new EconomyResponse(0, 0, EconomyResponse.ResponseType.FAILURE, "Bank not found.");

        if (bank.getOwner().getUuid().equals(dbPlayer.getUuid())) {
            return new EconomyResponse(0, 0, EconomyResponse.ResponseType.SUCCESS, "Is owner.");

        } else {
            return new EconomyResponse(1, 0, EconomyResponse.ResponseType.FAILURE, "Not owner.");
        }
    }

    @Override
    public EconomyResponse isBankOwner(String name, String playerName) {
        try (Session session = sessionBuilder.openSession()) {
            DBPlayer dbPlayer = CommonsAPI.getInstance().getPlayerFromDB(session, playerName);
            if (dbPlayer == null)
                return new EconomyResponse(0, 0, EconomyResponse.ResponseType.FAILURE, "Player not found");

            return isBankOwner(session, dbPlayer, name);
        }
    }

    @Override
    public EconomyResponse isBankOwner(String name, OfflinePlayer player) {
        try (Session session = sessionBuilder.openSession()) {
            DBPlayer dbPlayer = CommonsAPI.getInstance().getPlayerFromDB(session, player.getUniqueId());
            if (dbPlayer == null)
                return new EconomyResponse(0, 0, EconomyResponse.ResponseType.FAILURE, "Player not found");

            return isBankOwner(session, dbPlayer, name);
        }
    }


    private EconomyResponse isBankMember(Session session, DBPlayer dbPlayer, String bankName) {
        boolean isMember = DBQueries.checkPlayerIsBankMember(session, dbPlayer.getUuid(), bankName);

        if (isMember) {
            return new EconomyResponse(0, 0, EconomyResponse.ResponseType.SUCCESS, "Success.");

        } else {
            return new EconomyResponse(0, 0, EconomyResponse.ResponseType.FAILURE, "Failure.");
        }
    }

    @Override
    public EconomyResponse isBankMember(String name, String playerName) {
        try (Session session = sessionBuilder.openSession()) {
            DBPlayer dbPlayer = CommonsAPI.getInstance().getPlayerFromDB(session, playerName);
            if (dbPlayer == null)
                return new EconomyResponse(0, 0, EconomyResponse.ResponseType.FAILURE, "Player not found");

            return isBankMember(session, dbPlayer, name);
        }
    }

    @Override
    public EconomyResponse isBankMember(String name, OfflinePlayer player) {
        try (Session session = sessionBuilder.openSession()) {
            DBPlayer dbPlayer = CommonsAPI.getInstance().getPlayerFromDB(session, player.getUniqueId());
            if (dbPlayer == null)
                return new EconomyResponse(0, 0, EconomyResponse.ResponseType.FAILURE, "Player not found");

            return isBankMember(session, dbPlayer, name);
        }
    }


    @Override
    public List<String> getBanks() {
        return sessionBuilder.execute(DBQueries::getAllBankNames);
    }

    // --- Create normal account ---

    private boolean createPlayerAccount(Session session, DBPlayer dbPlayer, String serviceName) {
        DBService dbService = CommonsAPI.getInstance().getActiveServiceFromDB(session, serviceName);
        if (dbPlayer == null)
            return false;

        Transaction tx = session.beginTransaction();
        ServiceBalance serviceBalance = new ServiceBalance(dbPlayer, dbService);
        session.persist(serviceBalance);
        tx.commit();

        return true;
    }

    @Override
    public boolean createPlayerAccount(String playerName) {
        return false;
    }

    @Override
    public boolean createPlayerAccount(OfflinePlayer player) {
        return false;
    }

    @Override
    public boolean createPlayerAccount(String playerName, String serviceName) {
        try (Session session = sessionBuilder.openSession()) {
            DBPlayer dbPlayer = CommonsAPI.getInstance().getPlayerFromDB(session, playerName);
            if (dbPlayer == null)
                return false;

            return createPlayerAccount(session, dbPlayer, serviceName);
        }
    }

    @Override
    public boolean createPlayerAccount(OfflinePlayer player, String serviceName) {
        try (Session session = sessionBuilder.openSession()) {
            DBPlayer dbPlayer = CommonsAPI.getInstance().getPlayerFromDB(session, player.getUniqueId());
            if (dbPlayer == null)
                return false;

            return createPlayerAccount(session, dbPlayer, serviceName);
        }
    }

    // --- Own methods ---


    @Override
    public EconomyResponse changeBankOwner(String bankName, String newOwnerName) {
        try (Session session = sessionBuilder.openSession()) {
            Bank bank = session.get(Bank.class, bankName);
            if (bank == null)
                return new EconomyResponse(0, 0, EconomyResponse.ResponseType.FAILURE, "Bank not found.");

            DBPlayer newOwner = CommonsAPI.getInstance().getPlayerFromDB(session, newOwnerName);
            if (newOwner == null)
                return new EconomyResponse(1, 0, EconomyResponse.ResponseType.FAILURE, "Player not found.");

            Transaction tx = session.beginTransaction();
            bank.setOwner(newOwner);
            session.update(bank);
            tx.commit();

            return new EconomyResponse(0, 0, EconomyResponse.ResponseType.SUCCESS, "Success.");
        }
    }

    @Override
    public EconomyResponse addBankMember(String bankName, String playerName) {
        try (Session session = sessionBuilder.openSession()) {
            Bank bank = session.get(Bank.class, bankName);
            if (bank == null)
                return new EconomyResponse(0, 0, EconomyResponse.ResponseType.FAILURE, "Bank not found.");

            DBPlayer player = CommonsAPI.getInstance().getPlayerFromDB(session, playerName);
            if (player == null)
                return new EconomyResponse(1, 0, EconomyResponse.ResponseType.FAILURE, "Player not found.");

            if (bank.getOwner().getUuid().equals(player.getUuid()))
                return new EconomyResponse(2, 0, EconomyResponse.ResponseType.FAILURE, "Player is the owner.");

            if (bank.hasMember(player))
                return new EconomyResponse(3, 0, EconomyResponse.ResponseType.FAILURE, "Player already a member.");

            Transaction tx = session.beginTransaction();
            PlayerToBank ptb = new PlayerToBank(player, bank);
            bank.getMembers().add(ptb);
            session.persist(ptb);
            session.update(bank);
            tx.commit();

            return new EconomyResponse(0, 0, EconomyResponse.ResponseType.SUCCESS, "Success.");
        }
    }

    @Override
    public EconomyResponse removeBankMember(String bankName, String playerName) {
        try (Session session = sessionBuilder.openSession()) {
            Bank bank = session.get(Bank.class, bankName);
            if (bank == null)
                return new EconomyResponse(0, 0, EconomyResponse.ResponseType.FAILURE, "Bank not found.");

            DBPlayer player = CommonsAPI.getInstance().getPlayerFromDB(session, playerName);
            if (player == null)
                return new EconomyResponse(1, 0, EconomyResponse.ResponseType.FAILURE, "Player not found.");

            if (!bank.hasMember(player))
                return new EconomyResponse(2, 0, EconomyResponse.ResponseType.FAILURE, "Player is no member.");

            Transaction tx = session.beginTransaction();
            for (PlayerToBank member : bank.getMembers()) {
                if (member.getPlayer().getUuid().equals(player.getUuid())) {
                    session.delete(member);
                }
            }
            session.persist(bank);
            tx.commit();

            return new EconomyResponse(0, 0, EconomyResponse.ResponseType.SUCCESS, "Success.");
        }
    }

    @CheckForNull
    @Override
    public String getBankOwner(String bankName) {
        try (Session session = sessionBuilder.openSession()) {
            Bank bank = session.get(Bank.class, bankName);
            if (bank == null)
                return null;

            return bank.getOwner().getName();
        }
    }

    @CheckForNull
    @Override
    public List<String> getBankMembers(String bankName) {
        try (Session session = sessionBuilder.openSession()) {
            Bank bank = session.get(Bank.class, bankName);
            if (bank == null)
                return null;

            return bank.getMembers().stream()
                    .map(p -> p.getPlayer().getName())
                    .collect(Collectors.toList());
        }
    }
}
