package de.derteufelqwe.ServerManager.cli.server;

import de.derteufelqwe.ServerManager.DBQueries;
import de.derteufelqwe.ServerManager.ServerManager;
import de.derteufelqwe.ServerManager.tablebuilder.Column;
import de.derteufelqwe.ServerManager.tablebuilder.TableBuilder;
import de.derteufelqwe.commons.hibernate.ITypedSessionRunnable;
import de.derteufelqwe.commons.hibernate.SessionBuilder;
import de.derteufelqwe.commons.hibernate.objects.DBContainer;
import lombok.extern.log4j.Log4j2;
import org.hibernate.Session;
import picocli.CommandLine;

import java.util.List;

@CommandLine.Command(name = "list", description = "Lists Minecraft / BungeeCord containers")
@Log4j2
public class ListContainersCmd implements Runnable {

    private final SessionBuilder sessionBuilder = ServerManager.getSessionBuilder();

    @CommandLine.Option(names = {"-a", "--all"}, description = "Also show stopped containers")
    private boolean showAll = false;

    @CommandLine.Option(names = {"-s", "--service"}, description = "Only show containers for a single service")
    private String serviceID = null;


    @Override
    public void run() {
        TableBuilder tableBuilder = new TableBuilder()
                .withNoRowSeparation()
                .withColumn(new Column.Builder()
                        .withTitle("ID")
                        .withMaxWidth(20)
                        .build())
                .withColumn(new Column.Builder()
                        .withTitle("Name")
                        .withMaxWidth(30)
                        .build())
                .withColumn(new Column.Builder()
                        .withTitle("Type")
                        .build())
                .withColumn(new Column.Builder()
                        .withTitle("Service")
                        .build())
                .withColumn(new Column.Builder()
                        .withTitle("Health")
                        .build());


        sessionBuilder.execute(session -> {
            List<DBContainer> containers = DBQueries.getContainers(session, showAll, serviceID);

            for(DBContainer container : containers) {
                String healthMsg = "Healthy";
                if (!container.isActive()) {
                    healthMsg = "Stopped";
                } else if (!container.isHealthy()) {
                    healthMsg = "Unhealthy";
                }

                tableBuilder.addToColumn(0, container.getId());
                tableBuilder.addToColumn(1, container.getName());
                tableBuilder.addToColumn(2, container.getService().getType());
                tableBuilder.addToColumn(3, container.getService().getId());
                tableBuilder.addToColumn(4, healthMsg);
            }

            tableBuilder.build(log);
        });
    }
}
