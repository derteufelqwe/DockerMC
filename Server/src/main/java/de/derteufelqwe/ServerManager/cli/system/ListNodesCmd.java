package de.derteufelqwe.ServerManager.cli.system;

import com.github.dockerjava.api.model.Info;
import com.google.inject.Inject;
import de.derteufelqwe.ServerManager.DBQueries;
import de.derteufelqwe.ServerManager.Docker;
import de.derteufelqwe.ServerManager.ServerManager;
import de.derteufelqwe.ServerManager.Testing;
import de.derteufelqwe.ServerManager.config.MainConfig;
import de.derteufelqwe.ServerManager.tablebuilder.Column;
import de.derteufelqwe.ServerManager.tablebuilder.TableBuilder;
import de.derteufelqwe.commons.config.Config;
import de.derteufelqwe.commons.hibernate.LocalSessionRunnable;
import de.derteufelqwe.commons.hibernate.SessionBuilder;
import de.derteufelqwe.commons.hibernate.objects.Node;
import lombok.extern.log4j.Log4j2;
import org.hibernate.Session;
import picocli.CommandLine;

import java.util.ArrayList;
import java.util.List;

@CommandLine.Command(name = "listNodes", description = "Lists docker nodes")
@Log4j2
public class ListNodesCmd implements Runnable {

    @Inject
    private Docker docker;
    @Inject
    private SessionBuilder sessionBuilder;
    @Inject
    private Config<MainConfig> mainCfg;


    @CommandLine.Option(names = {"-a", "--all"}, description = "Also show removed nodes")
    private boolean all = false;

    @Override
    public void run() {
        Info info = docker.getDocker().infoCmd().exec();
        String currentNodeId = info.getSwarm().getNodeID();

        TableBuilder tableBuilder = new TableBuilder()
                .withNoRowSeparation()
                .withColumn(new Column.Builder()
                        .withTitle("NodeID")
                        .build())
                .withColumn(new Column.Builder()
                        .withTitle("Name")
                        .build())
                .withColumn(new Column.Builder()
                        .withTitle("Available")
                        .build())
                .withColumn(new Column.Builder()
                        .withTitle("IP")
                        .build())
                .withColumn(new Column.Builder()
                        .withTitle("Role")
                        .build())
                ;

        List<Node> nodes = sessionBuilder.execute(session -> {
            if (all) {
                return DBQueries.getAllNodes(session);

            } else {
                return DBQueries.getAllActiveNodes(session);
            }
        });


        for (Node node : nodes) {
            String idPrefix = node.getId().equals(currentNodeId) ? "> " : "";

            tableBuilder.addToColumn(0, idPrefix + node.getId());
            tableBuilder.addToColumn(1, node.getName());
            tableBuilder.addToColumn(2, node.getLeaveTime() == null ? "Yes" : "No");
            tableBuilder.addToColumn(3, node.getIp());
            tableBuilder.addToColumn(4, node.isManager() ? "master" : "worker");
        }

        tableBuilder.build(log);
    }
}
