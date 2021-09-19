package de.derteufelqwe.ServerManager.setup.infrastructure;

import com.password4j.BCryptFunction;
import de.derteufelqwe.ServerManager.config.MainConfig;
import de.derteufelqwe.ServerManager.config.objects.CertificateCfg;
import de.derteufelqwe.ServerManager.exceptions.FatalDockerMCError;
import de.derteufelqwe.commons.Constants;
import de.derteufelqwe.commons.Utils;
import de.derteufelqwe.commons.exceptions.DockerMCException;
import lombok.SneakyThrows;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

@Log4j2
public class RegistryCertificates {

    private MainConfig mainConfig;


    public RegistryCertificates(MainConfig mainConfig) {
        this.mainConfig = mainConfig;
    }


    public RegistryCertFiles find() {
        return new RegistryCertFiles();
    }


    @SneakyThrows
    public RegistryCertFiles create() {
        Path tmpDir = Files.createTempDirectory("DMC");
        String tmpPath = tmpDir + "/";

        createHtpasswdFile(tmpDir);
        createCertificate(tmpDir);

        FileUtils.copyFile(new File(tmpPath + Constants.REGISTRY_CERT_NAME), new File(Constants.REGISTRY_CERT_PATH + Constants.REGISTRY_CERT_NAME));
        FileUtils.copyFile(new File(tmpPath + Constants.REGISTRY_KEY_NAME), new File(Constants.REGISTRY_CERT_PATH + Constants.REGISTRY_KEY_NAME));
        FileUtils.copyFile(new File(tmpPath + Constants.REGISTRY_HTPASSWD_NAME), new File(Constants.REGISTRY_CERT_PATH + Constants.REGISTRY_HTPASSWD_NAME));

        return new RegistryCertFiles();
    }

    public void destroy() {
        RegistryCertFiles registryCertFiles = new RegistryCertFiles();
        registryCertFiles.deleteFiles();
    }


    private void createCertificate(Path tmpDir) {
        CertificateCfg cfg = mainConfig.getRegistryCerCfg();

        String sslCertConfig;
        try {
            sslCertConfig = IOUtils.resourceToString("sslCert.cnf", StandardCharsets.UTF_8, getClass().getClassLoader());

        } catch (IOException e) {
            throw new FatalDockerMCError("Failed to read sslCert.cnf file");
        }

        sslCertConfig = sslCertConfig
                .replaceFirst("\\{C\\}", cfg.getCountryCode())
                .replaceFirst("\\{ST\\}", cfg.getState())
                .replaceFirst("\\{L\\}", cfg.getCity())
                .replaceFirst("\\{O\\}", cfg.getOrganizationName())
                .replaceFirst("\\{M\\}", cfg.getEmail());

        File confFile = new File(tmpDir + "/sslCert.cnf");
        try {
            FileUtils.write(confFile, sslCertConfig, StandardCharsets.UTF_8);

        } catch (IOException e) {
            throw new FatalDockerMCError("Failed to write to " + confFile.getAbsolutePath());
        }

        String output = Utils.executeCommandOnHost("openssl", "req", "-x509", "-newkey", "rsa:4096", "-nodes", "-sha256", "-days", "356",
                "-out", tmpDir + "/" + Constants.REGISTRY_CERT_NAME, "-keyout", tmpDir + "/" + Constants.REGISTRY_KEY_NAME,
                "-config", confFile.getAbsolutePath());

        File certFile = new File(tmpDir + "/" + Constants.REGISTRY_CERT_NAME);
        File keyFile = new File(tmpDir + "/" + Constants.REGISTRY_KEY_NAME);
        if (!certFile.exists() || !keyFile.exists()) {
            throw new DockerMCException("Failed to create config file. Logs: \n" + output);
        }
    }

    @SneakyThrows
    private void createHtpasswdFile(Path tmpDir) {
        String htpasswd = mainConfig.getRegistryUsername() + ":" + BCryptFunction.getInstance(5).hash(mainConfig.getRegistryPassword()).getResult();
        File htpasswdFile = new File(tmpDir + "/htpasswd");
        FileUtils.write(htpasswdFile, htpasswd, StandardCharsets.UTF_8);
    }


    /**
     * Utility class to work with the certificates for the registry
     */
    public static class RegistryCertFiles {

        public final File caCrt = new File(Constants.REGISTRY_CERT_PATH + "ca.crt");
        public final File caKey = new File(Constants.REGISTRY_CERT_PATH + "ca.key");
        public final File htpasswd = new File(Constants.REGISTRY_CERT_PATH + "htpasswd");

        public RegistryCertFiles() {
        }

        public boolean filesExist() {
            return caCrt.exists() && caKey.exists() && htpasswd.exists();
        }

        @SuppressWarnings("all")
        public void deleteFiles() {
            caCrt.delete();
            caKey.delete();
            htpasswd.delete();
        }

    }


}
