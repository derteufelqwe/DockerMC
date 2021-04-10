package de.derteufelqwe.ServerManager.cli.system;

import de.derteufelqwe.ServerManager.ServerManager;
import de.derteufelqwe.ServerManager.utils.Commons;
import de.derteufelqwe.commons.Constants;
import lombok.extern.log4j.Log4j2;
import picocli.CommandLine;
import sun.security.x509.X509CertImpl;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Date;

@CommandLine.Command(name = "registryCertInfos", aliases = {"regCrtInfos"}, description = "Displays information about the registry ssl certificate")
@Log4j2
public class RegistryCertInfosCmd implements Runnable {

    private final Commons commons = ServerManager.getCommons();

    @Override
    public void run() {
        try {
            File certFile = new File(Constants.REGISTRY_CERT_PATH_1 + Constants.REGISTRY_CERT_NAME);
            X509Certificate certificate = new X509CertImpl(new FileInputStream(certFile));

            log.info("SSL certificate information:");
            log.info(String.format("Name         : %s", Constants.REGISTRY_CERT_NAME));
            log.info(String.format("Start        : %s", certificate.getNotBefore().toString()));
            if (certificate.getNotAfter().before(new Date(System.currentTimeMillis())))
                log.error(String.format("Expiration   : %s (Expired)", certificate.getNotAfter().toString()));
            else
                log.info(String.format("Expiration   : %s", certificate.getNotAfter().toString()));
            log.info(String.format("Serialnumber : %s", certificate.getSerialNumber().toString()));
            log.info(String.format("Path         : %s", "'/etc/docker/certs.d/" + Constants.REGISTRY_URL + "'"));

        } catch (IOException e) {
            log.warn("Certificate not found! Make sure it's generated.");

        } catch (CertificateException e) {
            log.error("Invalid certificate. Try to regenerate it. Error: {}.", e.getMessage());
        }
    }
}
