package de.derteufelqwe.ServerManager.commands.status;

import com.github.dockerjava.api.model.Container;
import de.derteufelqwe.commons.Constants;
import de.derteufelqwe.ServerManager.ServerManager;
import de.derteufelqwe.commons.Utils;
import picocli.CommandLine;

import java.util.List;

/**
 * Command to list all currently running BungeeCord Proxies.
 */
@CommandLine.Command(name = "list", description = "Lists Proxies",
        mixinStandardHelpOptions = true)
class ProxiesList implements Runnable {

    private final int ALL_Proxies_LIMIT = 10000;

    @CommandLine.Option(names = {"-a", "--all"}, description = "Show all proxies (might be a super long list.")
    private boolean showAll = false;

    @CommandLine.Option(names = {"-l", "--limit"}, description = "Set the limit of displayed proxies. Default is 25.")
    private int limit = 25;


    @Override
    public void run() {
        if (showAll) {
            limit = ALL_Proxies_LIMIT;
        }

        List<Container> containers = ServerManager.getDocker().getDocker().listContainersCmd()
                .withLimit(limit)
                .withLabelFilter(Utils.quickLabel(Constants.ContainerType.BUNGEE))
                .exec();

        System.out.println("List of Proxies: TDB... (" + containers.size() + ")");
    }
}