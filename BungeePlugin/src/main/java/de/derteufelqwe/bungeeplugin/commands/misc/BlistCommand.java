package de.derteufelqwe.bungeeplugin.commands.misc;

import com.orbitz.consul.CatalogClient;
import de.derteufelqwe.bungeeplugin.BungeePlugin;
import de.derteufelqwe.bungeeplugin.redis.RedisDataManager;
import de.derteufelqwe.commons.CommonsAPI;
import de.derteufelqwe.commons.hibernate.SessionBuilder;
import de.derteufelqwe.commons.hibernate.objects.DBContainer;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.plugin.Command;
import org.hibernate.Session;

import java.util.Arrays;
import java.util.List;


/**
 * This command lists all available BungeeCord instances.
 */
public class BlistCommand extends Command {

    private final int SERVERS_PER_PAGE = 10;
    private final String PREFIX = ChatColor.YELLOW + "[BList]" + ChatColor.RESET;

    private SessionBuilder sessionBuilder = BungeePlugin.getSessionBuilder();
    private RedisDataManager redisDataManager = BungeePlugin.getRedisDataManager();


    public BlistCommand(CatalogClient catalogClient) {
        super("blist", "bungeecord.command.blist");
    }


    private void send(CommandSender sender, String msg, Object... args) {
        sender.sendMessage(new TextComponent(String.format(msg, args)));
    }


    @Override
    public void execute(CommandSender sender, String[] rawArgs) {
        List<String> args = Arrays.asList(rawArgs);

        int page = 0;
        if (args.size() > 0) {
            try {
                page = Integer.parseInt(args.get(0));

            } catch (NumberFormatException e) {
                // Pass
            }
        }
        if (page < 1)
            page = 1;

        this.blist(sender, page);
    }


    public void blist(CommandSender sender, int page) {
        List<DBContainer> bungeeContainers;
        try (Session session = sessionBuilder.openSession()) {
            bungeeContainers = CommonsAPI.getInstance().getRunningBungeeContainersFromDB(session);
        }

        send(sender, PREFIX + " --- BungeeCord Instances ---", this.PREFIX);

        for (DBContainer container : bungeeContainers) {
            int playerCount = redisDataManager.getBungeesPlayerCount(container.getName());
            String youMarker = "";

            if (container.getName().equals(BungeePlugin.BUNGEECORD_ID)) {
                youMarker = ChatColor.GOLD + "> " + ChatColor.RESET;
            }

            send(sender, "%s%s %s(%s)", youMarker, container.getName(), ChatColor.YELLOW, playerCount);
        }

        send(sender, "%s BungeeCord count: %s", PREFIX, bungeeContainers.size());
    }


}
