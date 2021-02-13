package minecraftplugin.minecraftplugin.economy;

import net.milkbowl.vault.economy.Economy;
import net.milkbowl.vault.economy.EconomyResponse;

import java.util.List;

public interface DMCEconomy extends Economy {

    public EconomyResponse changeBankOwner(String bankName, String newOwnerName);

    public EconomyResponse addBankMember(String bankName, String playerName);

    public EconomyResponse removeBankMember(String bankName, String playerName);

    public String getBankOwner(String bankName);

    public List<String> getBankMembers(String bankName);

}
