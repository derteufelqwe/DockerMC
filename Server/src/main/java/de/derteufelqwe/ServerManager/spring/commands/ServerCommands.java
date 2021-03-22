package de.derteufelqwe.ServerManager.spring.commands;

import com.github.dockerjava.api.model.Service;
import de.derteufelqwe.ServerManager.Docker;
import de.derteufelqwe.ServerManager.spring.Commons;
import de.derteufelqwe.ServerManager.tablebuilder.Column;
import de.derteufelqwe.ServerManager.tablebuilder.TableBuilder;
import de.derteufelqwe.ServerManager.utils.HelpBuilder;
import de.derteufelqwe.ServerManager.utils.Utils;
import de.derteufelqwe.commons.CommonsAPI;
import de.derteufelqwe.commons.Constants;
import de.derteufelqwe.commons.hibernate.SessionBuilder;
import de.derteufelqwe.commons.hibernate.objects.DBContainer;
import de.derteufelqwe.commons.hibernate.objects.Notification;
import lombok.SneakyThrows;
import lombok.extern.log4j.Log4j2;
import org.hibernate.Session;
import org.jline.reader.LineReader;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.shell.standard.ShellComponent;
import org.springframework.shell.standard.ShellMethod;
import org.springframework.shell.standard.ShellOption;

import java.io.IOException;
import java.io.InvalidClassException;
import java.util.List;

@ShellComponent
@Log4j2
public class ServerCommands {

    @Autowired
    private ApplicationEventPublisher applicationEventPublisher;
    @Autowired
    private Docker docker;
    @Autowired
    private StringRedisTemplate redisTemplate;
    @Autowired
    private SessionBuilder sessionBuilder;
    @Autowired @Lazy
    private LineReader lineReader;
    @Autowired
    private Commons commons;


    @ShellMethod(value = "Shows the help", key = "server")
    private void showHelp() {
        System.out.println("Manage servers");
        System.out.println("");

        new HelpBuilder("Commands:")
                .addEntry("help", "Shows this help")
                .addEntry("list", "Lists all running containers from the DB")
                .addEntry("lobbyserver", "Displays the name of the LobbyServer")
                .print();
    }

    @ShellMethod(value = "Shows the help", key = "server help")
    private void showHelp2() {
        showHelp();
    }


    @ShellMethod(value = "Lists all minecraft containers", key = "server list")
    private void listContainers() {
        try (Session session = sessionBuilder.openSession()) {
            List<DBContainer> containers = session.createNativeQuery(
                    "SELECT * FROM containers AS c WHERE c.exitcode IS NULL ORDER BY c.service_id",
                    DBContainer.class).getResultList();

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

            for(DBContainer container : containers) {
                String healthMsg = container.isHealthy() ? "Healthy" : "Unhealthy";

                tableBuilder.addToColumn(0, container.getId());
                tableBuilder.addToColumn(1, container.getName());
                tableBuilder.addToColumn(2, container.getService().getType());
                tableBuilder.addToColumn(3, container.getService().getName());
                tableBuilder.addToColumn(4, healthMsg);
            }

            tableBuilder.build(log);
        }
    }

    @ShellMethod(value = "Returns the currently configured lobby server", key = "server lobbyserver")
    private void getLobbyServerName() {
        String lobbyServer = redisTemplate.opsForValue().get(Constants.REDIS_KEY_LOBBYSERVER);

        if (lobbyServer == null || lobbyServer.equals(""))
            log.warn("LobbyServer name not configured.");
        else
            log.info("LobbyServer name: '{}'.", lobbyServer);
    }




    @SneakyThrows
    @ShellMethod(value = "testing", key = "tests")
    public void test() {
        try (Session session = sessionBuilder.openSession()) {
            Notification notification = session.get(Notification.class, 3L);
            System.out.println("sd");
        }
    }

}
