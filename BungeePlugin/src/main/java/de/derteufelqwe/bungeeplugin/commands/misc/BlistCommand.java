package de.derteufelqwe.bungeeplugin.commands.misc;

import com.orbitz.consul.CatalogClient;
import com.orbitz.consul.model.ConsulResponse;
import com.orbitz.consul.model.catalog.CatalogService;
import de.derteufelqwe.bungeeplugin.BungeePlugin;
import de.derteufelqwe.bungeeplugin.redis.RedisDataManager;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.plugin.Command;

import java.util.Arrays;
import java.util.List;


/**
 * This command lists all available BungeeCord instances.
 */
public class BlistCommand extends Command {

    private final int SERVERS_PER_PAGE = 10;
    private final String PREFIX = ChatColor.YELLOW + "[BList]" + ChatColor.RESET;
    private RedisDataManager redisDataManager = BungeePlugin.getRedisDataManager();

    private CatalogClient catalogClient;


    public BlistCommand(CatalogClient catalogClient) {
        super("blist", "bungeecord.command.blist");
        this.catalogClient = catalogClient;
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
        ConsulResponse<List<CatalogService>> response = catalogClient.getService("bungeecord");
        List<CatalogService> services = response.getResponse();
        int allServicesCount = services.size();
        int maxPages = (int) Math.ceil(services.size() / (double) SERVERS_PER_PAGE);

        if (page > maxPages) {
            page = maxPages;
        }

        int listEnd = Math.min(SERVERS_PER_PAGE * page, services.size());
        services = services.subList(SERVERS_PER_PAGE * (page - 1), listEnd);

        sender.sendMessage(new TextComponent(String.format(
                "%s --- Page %s/%s ---", this.PREFIX, page, maxPages
        )));

        for (CatalogService service : services) {
            int playerCount = redisDataManager.getBungeesPlayerCount(service.getServiceId());
            String youMarker = "";
            if (service.getServiceId().equals(BungeePlugin.BUNGEECORD_ID)) {
                youMarker = ChatColor.GOLD + "> " + ChatColor.RESET;
            }

            sender.sendMessage(new TextComponent(String.format(
                    "%s%s %s(%s)", youMarker, service.getServiceId(), ChatColor.YELLOW, playerCount
            )));
        }

        sender.sendMessage(new TextComponent(String.format(
                "%s BungeeCord count: %s", PREFIX, allServicesCount
        )));

    }


}
