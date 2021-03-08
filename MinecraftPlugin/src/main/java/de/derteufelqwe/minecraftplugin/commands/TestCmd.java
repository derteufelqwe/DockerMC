package de.derteufelqwe.minecraftplugin.commands;


import co.aikar.commands.BaseCommand;
import co.aikar.commands.annotation.CatchUnknown;
import co.aikar.commands.annotation.CommandAlias;
import co.aikar.commands.annotation.Default;
import de.derteufelqwe.commons.config.Config;
import de.derteufelqwe.minecraftplugin.MinecraftPlugin;
import de.derteufelqwe.minecraftplugin.config.SignConfig;
import de.derteufelqwe.minecraftplugin.config.TPSign;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.command.CommandSender;


@CommandAlias("mtest")
public class TestCmd extends BaseCommand {

    @Default
    @CatchUnknown
    public void defaultt(CommandSender sender) {
        TPSign sign = new TPSign("name", new Location(Bukkit.getWorld("world"), 0, 0, 0), "dest-1");
        Config<SignConfig> cfg = MinecraftPlugin.getSIGN_CONFIG();
        if (cfg.get().getSigns().size() == 0)
            cfg.get().addSign(sign);

        cfg.save();

        System.out.println("done");
    }

}
