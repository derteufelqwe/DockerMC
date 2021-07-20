package de.derteufelqwe.ServerManager.cli.service;

import com.google.inject.Inject;
import de.derteufelqwe.ServerManager.ServerManager;
import de.derteufelqwe.ServerManager.tablebuilder.Column;
import de.derteufelqwe.ServerManager.tablebuilder.TableBuilder;
import de.derteufelqwe.ServerManager.utils.ServiceHealthAnalyzer;
import de.derteufelqwe.commons.hibernate.LocalSessionRunnable;
import de.derteufelqwe.commons.hibernate.SessionBuilder;
import de.derteufelqwe.commons.hibernate.objects.DBService;
import de.derteufelqwe.commons.hibernate.objects.DBServiceHealth;
import lombok.extern.log4j.Log4j2;
import org.hibernate.Session;
import picocli.CommandLine;

import java.time.Duration;
import java.util.List;
import java.util.stream.Collectors;

@CommandLine.Command(name = "details", description = "Show details about a certain service")
@Log4j2
public class ServiceDetailsCmd implements Runnable {

    @Inject private SessionBuilder sessionBuilder;


    @CommandLine.Parameters(description = "ID of the service to get details about")
    private String serviceID = null;

    @CommandLine.Option(names = {"-d", "--duration"}, defaultValue = "30m")
    private Duration errorDuration;

    @Override
    public void run() {
        TableBuilder tableBuilder = new TableBuilder()
                .withColumn(new Column.Builder()
                        .withTitle("Attribute")
                        .build())
                .withColumn(new Column.Builder()
                        .withTitle("Value")
                        .build());

        new LocalSessionRunnable(sessionBuilder) {
            @Override
            protected void exec(Session session) {
                DBService dbService = session.get(DBService.class, serviceID);
                if (dbService == null) {
                    log.error("Service {} not found.", serviceID);
                    return;
                }

                // --- Log the infos ---
                tableBuilder.addToColumn(0, "ID");
                tableBuilder.addToColumn(1, dbService.getId());

                tableBuilder.addToColumn(0, "Name");
                tableBuilder.addToColumn(1, dbService.getName());

                tableBuilder.addToColumn(0, "Replicas");
                tableBuilder.addToColumn(1, String.format("%s/%s (%s starting)", dbService.getRunningContainersCount(), dbService.getReplicas(), dbService.getStartingContainersCount()));

                tableBuilder.addToColumn(0, "Health");
                if (dbService.isActive()) {
                    List<DBServiceHealth> healths = new ServiceHealthAnalyzer(session, dbService.getId())
                            .analyze((int) errorDuration.getSeconds());
                    if (healths.size() == 0) {
                        tableBuilder.addToColumn(1, "(Healthy)");

                    } else {
                        String errors = healths.stream()
                                .map(DBServiceHealth::getError)
                                .collect(Collectors.joining("\n"));

                        tableBuilder.addToColumn(1, errors);
                    }

                } else {
                    tableBuilder.addToColumn(1, "(Not active)");
                }

                tableBuilder.build(log);
            }
        }.run();

    }

}
