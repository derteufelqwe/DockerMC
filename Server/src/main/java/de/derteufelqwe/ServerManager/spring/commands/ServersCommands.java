package de.derteufelqwe.ServerManager.spring.commands;

import com.github.dockerjava.api.model.Service;
import de.derteufelqwe.ServerManager.Docker;
import de.derteufelqwe.ServerManager.spring.events.CheckInfrastructureEvent;
import de.derteufelqwe.ServerManager.spring.events.ReloadConfigEvent;
import de.derteufelqwe.commons.Constants;
import de.derteufelqwe.commons.hibernate.objects.DBContainer;
import lombok.extern.log4j.Log4j2;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
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
public class ServersCommands {

    @Autowired
    private ApplicationEventPublisher applicationEventPublisher;
    @Autowired
    private SessionFactory sessionFactory;
    @Autowired
    private Docker docker;
    @Autowired
    private StringRedisTemplate redisTemplate;


    @ShellMethod(value = "Lists all running BungeeCord and Minecraft server", key = "server list-services")
    public void listServices() {
        String lobbyServerName = redisTemplate.opsForValue().get(Constants.REDIS_KEY_LOBBYSERVER);

        List<Service> services = docker.getDocker().listServicesCmd()
                .withLabelFilter(Collections.singletonMap(Constants.DOCKER_IDENTIFIER_KEY, Constants.DOCKER_IDENTIFIER_VALUE))
                .exec();

        String formatString = "%-20s | %-25s | %-10s ";
        log.info(String.format(formatString, "Name", "ID", "Type"));
        for (Service service : services) {
            String name = service.getSpec().getName();
            String lobbyAppend = name.equals(lobbyServerName) ? " (LobbyServer)" : "";
            log.info(String.format(formatString + lobbyAppend,
                    name,
                    service.getId(),
                    service.getSpec().getLabels().get(Constants.CONTAINER_IDENTIFIER_KEY))
            );
        }
    }

    @ShellMethod(value = "Lists all minecraft containers", key = "server list-containers")
    private void listContainers() {
        try (Session session = sessionFactory.openSession()) {
            List<DBContainer> containers = session.createNativeQuery(
                    "SELECT * FROM containers AS c WHERE c.exitcode IS NULL",
                    DBContainer.class).getResultList();

            String formatString = "%-20s | %-30s | %-14s | %-25s";
            log.info(String.format(formatString, "ID", "Name", "Type", "ServiceName"));
            log.info("---------------------------------------------------------------------------------------");
            for(DBContainer container : containers) {
                log.info(String.format(formatString,
                        container.getId().substring(0, 20),
                        container.getName().length() > 30 ? container.getName().substring(0, 30) : container.getName(),
                        container.getService().getType(),
                        container.getService().getName()
                    ));
            }

        }
    }

}
