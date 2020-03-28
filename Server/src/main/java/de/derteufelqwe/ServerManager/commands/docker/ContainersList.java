package de.derteufelqwe.ServerManager.commands.docker;

import com.github.dockerjava.api.model.Container;
import de.derteufelqwe.ServerManager.ServerManager;
import org.apache.commons.lang.StringUtils;
import picocli.CommandLine;

import java.util.List;

@CommandLine.Command(name = "list", description = "Lists Containers",
        mixinStandardHelpOptions = true)
class ContainersList implements Runnable {

    private final int ALL_CONTAINERS_LIMIT = 10000;

    @CommandLine.Option(names = {"-a", "--all"}, description = "Show all containers (might be a super long list.")
    private boolean showAll = false;

    @CommandLine.Option(names = {"-l", "--limit"}, description = "Set the limit of displayed containers. Default is 25.")
    private int limit = 25;


    @Override
    public void run() {
        if (showAll) {
            limit = ALL_CONTAINERS_LIMIT;
        }

        // ToDo: Add filter
        List<Container> containers = ServerManager.getDocker().getDocker().listContainersCmd()
                .withLimit(limit)
//                .withLabelFilter(de.derteufelqwe.commons.Constants.DOCKER_IDENTIFIER_TAG)
                .exec();

        System.out.println(
                StringUtils.center("Container ID", 64, '-') + "   " +
                StringUtils.center("Name", 63, '-')
        );

        String containerString = "";
        for (Container c : containers) {
            String id = c.getId();
            String name = c.getNames()[0].substring(1);
            containerString += id + "   " + name + "\n";
        }

        System.out.println(containerString);
    }
}