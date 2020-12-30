package de.derteufelqwe.bungeeplugin.commands;

import de.derteufelqwe.bungeeplugin.BungeePlugin;
import de.derteufelqwe.bungeeplugin.redis.RedisDataManager;
import de.derteufelqwe.commons.exceptions.NotFoundException;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.plugin.Command;
import org.apache.commons.cli.*;

import java.io.PrintWriter;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

/**
 * Bans a player from the whole network
 */
public class BanCommand extends Command {

    private RedisDataManager redisDataManager = BungeePlugin.getRedisDataManager();
    private Options options = new Options();
    private CommandLineParser parser;


    public BanCommand() {
        super("ban", "dockermc.bungee.ban");

        Option duration = new Option("d", "duration", true, "Duration of the ban");
        duration.setRequired(false);
        this.options.addOption(duration);

        this.parser = new DefaultParser();
    }


    @Override
    public void execute(CommandSender sender, String[] rawArgs) {
        List<String> args = Arrays.asList(rawArgs);

        if (args.size() < 2) {
            this.printHelp(sender);

        } else {
            try {
                String player = args.get(0);
                String reason = args.get(args.size() - 1);

                if (reason.startsWith("-")) {
                    sender.sendMessage(new TextComponent("Reason " + reason + " looks like an argument."));
                    return;
                }

                CommandLine cmd = parser.parse(options, args.subList(1, args.size() - 2).toArray(new String[0]));

                System.out.println("");
            } catch (ParseException e) {
                this.printHelp(sender);
            }
        }

    }


    private void printHelp(CommandSender sender) {
        sender.sendMessage(new TextComponent(ChatColor.RED + "/ban <username/uuid> [reason]"));
    }

    private void banPlayer(CommandSender sender, String targetPlayer, String reason) {



    }


}
