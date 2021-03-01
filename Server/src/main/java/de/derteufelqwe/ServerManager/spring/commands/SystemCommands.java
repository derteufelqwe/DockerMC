package de.derteufelqwe.ServerManager.spring.commands;

import de.derteufelqwe.ServerManager.Docker;
import de.derteufelqwe.ServerManager.spring.events.CheckInfrastructureEvent;
import de.derteufelqwe.ServerManager.spring.events.ReloadConfigEvent;
import de.derteufelqwe.commons.Constants;
import lombok.extern.log4j.Log4j2;
import org.apache.logging.log4j.core.tools.picocli.CommandLine;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ansi.AnsiColor;
import org.springframework.boot.ansi.AnsiColors;
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
import java.util.Date;

@ShellComponent
@Log4j2
@ShellCommandGroup(value = "system")
public class SystemCommands {

    @Autowired
    private ApplicationEventPublisher applicationEventPublisher;
    @Autowired
    private Docker docker;
    @Autowired
    private StringRedisTemplate redisTemplate;


    @ShellMethod(value = "Reloads and updates the servers config.", key = "system reload-config")
    public void reloadConfig() {
        ReloadConfigEvent reloadConfigEvent = new ReloadConfigEvent(this, ReloadConfigEvent.ReloadSource.COMMAND);
        applicationEventPublisher.publishEvent(reloadConfigEvent);

        if (reloadConfigEvent.isSuccess()) {
            log.info("Successfully reloaded server config.");

        } else {
            log.error("Config reload failed with: '{}'", reloadConfigEvent.getMessage());
        }
    }

    @ShellMethod(value = "Checks if all parts of the infrastructure are up and running or starts them.", key = "system check-infrastructure")
    public void checkInfrastructure() {
        CheckInfrastructureEvent infrastructureEvent = new CheckInfrastructureEvent(this, CheckInfrastructureEvent.ReloadSource.COMMAND);
        applicationEventPublisher.publishEvent(infrastructureEvent);

        if (infrastructureEvent.isSuccess()) {
            log.info("Infrastructure is up and running.");

        } else {
            log.error("Infrastructure setup failed. Solve the issues above to ensure full functionality.");
        }
    }

    @ShellMethod(value = "Prints information about the registry cert information. Most notably its expiration date.", key = "system registry-cert-infos")
    public void registryCertInfos() {
        try {
            File certFile = new File(Constants.REGISTRY_CERT_PATH_1 + Constants.REGISTRY_CERT_NAME);
            X509Certificate certificate = new X509CertImpl(new FileInputStream(certFile));

            log.info(String.format("Name         | %s", Constants.REGISTRY_CERT_NAME));
            log.info(String.format("Start        | %s", certificate.getNotBefore().toString()));
            if (certificate.getNotAfter().before(new Date(System.currentTimeMillis())))
                log.error(String.format("Expiration   | %s (Expired)", certificate.getNotAfter().toString()));
            else
                log.info(String.format("Expiration   | %s", certificate.getNotAfter().toString()));
            log.info(String.format("Serialnumber | %s", certificate.getSerialNumber().toString()));

        } catch (IOException e) {
            log.warn("Certificate not found! Make sure it's generated.");

        } catch (CertificateException e) {
            log.error("Invalid certificate. Try to regenerate it. Error: {}.", e.getMessage());
        }
    }

}
