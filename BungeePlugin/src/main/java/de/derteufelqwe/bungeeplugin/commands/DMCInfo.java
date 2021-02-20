package de.derteufelqwe.bungeeplugin.commands;

import co.aikar.commands.annotation.CommandAlias;
import co.aikar.commands.annotation.Subcommand;
import de.derteufelqwe.bungeeplugin.BungeePlugin;
import de.derteufelqwe.commons.hibernate.SessionBuilder;
import de.derteufelqwe.commons.hibernate.objects.DBContainer;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.CommandSender;
import org.hibernate.Session;

@CommandAlias("dmcinfo")
public class DMCInfo extends DMCBaseCommand {

    private final String PREFIX = ChatColor.YELLOW + "[DMCInfo] " + ChatColor.RESET;

    private SessionBuilder sessionBuilder = BungeePlugin.getSessionBuilder();


    @Subcommand("bungee")
    public void bungeeInfo(CommandSender sender) {
        try (Session session = sessionBuilder.openSession()) {
            DBContainer container = session.get(DBContainer.class, BungeePlugin.META_DATA.getContainerID());
            if (container == null) {
                send(sender, PREFIX + ChatColor.RED + "Container not found.");
                return;
            }

        }
    }


    @Subcommand("minecraft")
    public void minecraftInfo(CommandSender sender) {

    }

}
