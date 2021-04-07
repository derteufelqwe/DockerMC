package de.derteufelqwe.ServerManager.cli.service;

import com.github.dockerjava.api.model.Service;
import com.github.dockerjava.api.model.ServiceModeConfig;
import com.github.dockerjava.api.model.Task;
import com.github.dockerjava.api.model.TaskState;
import de.derteufelqwe.ServerManager.DBQueries;
import de.derteufelqwe.ServerManager.Docker;
import de.derteufelqwe.ServerManager.ServerManager;
import de.derteufelqwe.ServerManager.spring.Commons;
import de.derteufelqwe.ServerManager.tablebuilder.Column;
import de.derteufelqwe.ServerManager.tablebuilder.TableBuilder;
import de.derteufelqwe.ServerManager.utils.ServiceHealthAnalyzer;
import de.derteufelqwe.ServerManager.utils.ServiceHealthReader;
import de.derteufelqwe.ServerManager.utils.Utils;
import de.derteufelqwe.commons.CommonDBQueries;
import de.derteufelqwe.commons.Constants;
import de.derteufelqwe.commons.hibernate.LocalSessionRunnable;
import de.derteufelqwe.commons.hibernate.SessionBuilder;
import de.derteufelqwe.commons.hibernate.TypedSessionRunnable;
import de.derteufelqwe.commons.hibernate.objects.DBService;
import de.derteufelqwe.commons.hibernate.objects.DBServiceHealth;
import de.derteufelqwe.commons.hibernate.objects.Node;
import lombok.extern.log4j.Log4j2;
import org.hibernate.Session;
import picocli.CommandLine;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@CommandLine.Command(name = "list", description = "Lists all running services")
@Log4j2
public class ListServicesCmd implements Runnable {

    private Docker docker = ServerManager.getDocker();
    private SessionBuilder sessionBuilder = ServerManager.getSessionBuilder();


    @CommandLine.Option(names = {"-a", "--all"}, description = "Also show removed services")
    private boolean all = false;


    @Override
    public void run() {
        String lobbyServerName = "LobbyServer";

        TableBuilder tableBuilder = new TableBuilder()
                .withNoRowSeparation()
                .withColumn(new Column.Builder()
                        .withTitle("ID")
                        .build())
                .withColumn(new Column.Builder()
                        .withTitle("Name")
                        .build())
                .withColumn(new Column.Builder()
                        .withTitle("Replicas")
                        .build())
                .withColumn(new Column.Builder()
                        .withTitle("Health")
                        .build())
                .withColumn(new Column.Builder()
                        .withTitle("Type")
                        .build());

        if (all) {
            tableBuilder.withColumn(new Column.Builder()
                    .withTitle("Status")
                    .build());
        }


        new LocalSessionRunnable(sessionBuilder) {
            @Override
            protected void exec(Session session) {
                List<DBService> services = new ArrayList<>();
                if (all) {
                    services = DBQueries.getAllServices(session);

                } else {
                    services = CommonDBQueries.getActiveServices(session);
                }

                for (DBService service : services) {
                    String lobbyAppend = service.getName().equals(lobbyServerName) ? " (Lobby)" : "";

                    List<DBServiceHealth> healths = new ServiceHealthAnalyzer(session, service.getId()).analyze(180000);

                    tableBuilder.addToColumn(0, service.getId());
                    tableBuilder.addToColumn(1, service.getName());
                    tableBuilder.addToColumn(2, String.format("%s/%s", service.getRunningContainersCount(), service.getReplicas()));

                    if (healths.size() == 0) {
                        tableBuilder.addToColumn(3, "Healthy");

                    } else {
                        tableBuilder.addToColumn(3, "Unhealthy");
                    }
                    tableBuilder.addToColumn(4, service.getType() + lobbyAppend);

                    if (all) {
                        tableBuilder.addToColumn(5, service.isActive() ? "Active" : "Inactive");
                    }
                }
            }
        }.run();


        tableBuilder.build(log);
    }

}
