package minecraftplugin.minecraftplugin.commands;

import co.aikar.commands.BaseCommand;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.command.CommandSender;

public class DMCBaseCommand extends BaseCommand {

    protected void send(CommandSender sender, String msg, Object... args) {
        sender.sendMessage(new TextComponent(String.format(msg, args)));
    }

}
