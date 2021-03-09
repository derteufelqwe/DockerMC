package de.derteufelqwe.ServerManager.spring.commands;

import com.github.dockerjava.api.model.Service;
import de.derteufelqwe.ServerManager.Docker;
import de.derteufelqwe.ServerManager.ServerManager;
import de.derteufelqwe.ServerManager.spring.events.CheckInfrastructureEvent;
import de.derteufelqwe.ServerManager.spring.events.ReloadConfigEvent;
import de.derteufelqwe.ServerManager.tablebuilder.Column;
import de.derteufelqwe.ServerManager.tablebuilder.TableBuilder;
import de.derteufelqwe.commons.Constants;
import de.derteufelqwe.commons.hibernate.SessionBuilder;
import de.derteufelqwe.commons.hibernate.objects.DBContainer;
import lombok.extern.log4j.Log4j2;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.shell.standard.ShellCommandGroup;
import org.springframework.shell.standard.ShellComponent;
import org.springframework.shell.standard.ShellMethod;
import sun.security.x509.X509CertImpl;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Collections;
import java.util.Date;
import java.util.List;

@ShellComponent
@Log4j2
@ShellCommandGroup(value = "server")
public class ServerCommands {

    @Autowired
    private ApplicationEventPublisher applicationEventPublisher;
    @Autowired
    private Docker docker;
    @Autowired
    private StringRedisTemplate redisTemplate;
    @Autowired
    private SessionBuilder sessionBuilder;


    @ShellMethod(value = "Lists all minecraft containers", key = "server list-containers")
    private void listContainers() {
        try (Session session = sessionBuilder.openSession()) {
            List<DBContainer> containers = session.createNativeQuery(
                    "SELECT * FROM containers AS c WHERE c.exitcode IS NULL",
                    DBContainer.class).getResultList();

            TableBuilder tableBuilder = new TableBuilder()
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
                            .build());

            for(DBContainer container : containers) {
                tableBuilder.addToColumn(0, container.getId());
                tableBuilder.addToColumn(1, container.getName());
                tableBuilder.addToColumn(2, container.getService().getType());
                tableBuilder.addToColumn(3, container.getService().getName());
            }

            tableBuilder.build(log);
        }
    }

    @ShellMethod(value = "Returns the currently configured lobby server", key = "server get-lobbyserver")
    private void getLobbyServerName() {
        String lobbyServer = redisTemplate.opsForValue().get(Constants.REDIS_KEY_LOBBYSERVER);

        if (lobbyServer == null || lobbyServer.equals(""))
            log.warn("LobbyServer name not configured.");
        else
            log.info("LobbyServer name: '{}'.", lobbyServer);
    }

}
