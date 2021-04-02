package de.derteufelqwe.ServerManager.setup.infrastructure;

import com.github.dockerjava.api.command.CreateContainerResponse;
import com.github.dockerjava.api.command.WaitContainerResultCallback;
import com.github.dockerjava.api.model.Bind;
import com.github.dockerjava.api.model.HostConfig;
import com.github.dockerjava.api.model.Volume;
import de.derteufelqwe.ServerManager.Docker;
import de.derteufelqwe.ServerManager.ServerManager;
import de.derteufelqwe.ServerManager.config.MainConfig;
import de.derteufelqwe.ServerManager.config.objects.CertificateCfg;
import de.derteufelqwe.ServerManager.exceptions.FatalDockerMCError;
import de.derteufelqwe.ServerManager.setup.templates.DockerObjTemplate;
import de.derteufelqwe.commons.Constants;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Log4j2
public class RegistryCertificates {

    private MainConfig mainConfig = ServerManager.mainConfig.get();
    private Docker docker;


    public RegistryCertificates(Docker docker) {
        this.docker = docker;
    }


    public DockerObjTemplate.FindResponse find() {
        RegistryCertFiles registryCertFiles = new RegistryCertFiles();
        boolean exist = registryCertFiles.filesExist();

        if (exist) {
            return new DockerObjTemplate.FindResponse(true, null);

        } else {
            return new DockerObjTemplate.FindResponse(false, null);
        }
    }


    public DockerObjTemplate.CreateResponse create() {
        docker.pullImage(Constants.Images.OPENSSL.image());
        docker.pullImage(Constants.Images.HTPASSWD.image());
        this.destroy();     // Remove old files

        File sslCertConf;
        try {
             sslCertConf = this.createSSLConfFile();

        } catch (IOException e) {
            return new DockerObjTemplate.CreateResponse(false, null, e.getMessage());
        }

        try {
            DockerObjTemplate.CreateResponse response = this.createCertificate();

            try {
                this.createHtpasswdFile();

            } catch (IOException | FatalDockerMCError e) {
                return new DockerObjTemplate.CreateResponse(false, "", e.getMessage());
            }

            return response;

        } finally {
            sslCertConf.delete();
        }

    }

    public DockerObjTemplate.DestroyResponse destroy() {
        RegistryCertFiles registryCertFiles = new RegistryCertFiles();
        boolean exists = registryCertFiles.filesExist();
        registryCertFiles.deleteFiles();

        return new DockerObjTemplate.DestroyResponse(exists, null);
    }


    /**
     * Temporarily copies the sslCert.cnf file and fills its placeholders with the required values.
     * @return The file object of the config file
     * @throws IOException
     */
    private File createSSLConfFile() throws IOException {
        CertificateCfg cfg = mainConfig.getRegistryCerCfg();

        String sslCertConfig = IOUtils.resourceToString("sslCert.cnf", StandardCharsets.UTF_8, getClass().getClassLoader());
        sslCertConfig = sslCertConfig
                .replaceFirst("\\{C\\}", cfg.getCountryCode())      // County code
                .replaceFirst("\\{ST\\}", cfg.getState())           // State
                .replaceFirst("\\{L\\}", cfg.getCity())             // State
                .replaceFirst("\\{O\\}", cfg.getOrganizationName()) // State
                .replaceFirst("\\{M\\}", cfg.getEmail());           // State

        File file = new File(Constants.REGISTRY_CERT_PATH_1 + "/sslCert.cnf");
        FileUtils.write(file, sslCertConfig, StandardCharsets.UTF_8);

        return file;
    }

    /**
     * Generates the SSL certificate and key based on the sslCert.cnf config file.
     * @return Contains the container log if the generation failed.
     */
    private DockerObjTemplate.CreateResponse createCertificate() {
        Volume sslOutput = new Volume("/export");
        Bind bind = new Bind(Constants.REGISTRY_CERT_PATH_2, sslOutput);

        List<String> command = new ArrayList<>(Arrays.asList(
                "req", "-x509", "-newkey", "rsa:4096", "-nodes", "-sha256", "-days", "356",
                "-out", "/export/" + Constants.REGISTRY_CERT_NAME, "-keyout", "/export/" + Constants.REGISTRY_KEY_NAME,
                "-config", "/export/sslCert.cnf"));

        // Create and start the container, which generates the SSL certificate
        CreateContainerResponse response = docker.getDocker().createContainerCmd(Constants.Images.OPENSSL.image())
                .withVolumes(sslOutput)
                .withHostConfig(new HostConfig()
                        .withBinds(bind))
                .withCmd(command)
                .exec();

        log.debug("Certificate creation container: {}.", response.getId());
        docker.getDocker().startContainerCmd(response.getId()).exec();

        // Check if the container finished properly
        int exitCode = docker.getDocker().waitContainerCmd(response.getId())
                .exec(new WaitContainerResultCallback())
                .awaitStatusCode(10, TimeUnit.SECONDS);

        if (exitCode == 0) {
            return new DockerObjTemplate.CreateResponse(true, response.getId());
        }

        // Download the container log in case of failure
        String log = docker.getContainerLog(response.getId());

        return new DockerObjTemplate.CreateResponse(false, response.getId(), log);
    }

    /**
     * Generates the htpasswd file required to login into the registry.
     * @throws FatalDockerMCError Container exited with code != 0
     * @throws IOException File couldn't be saved.
     */
    private void createHtpasswdFile() throws FatalDockerMCError, IOException {
        CreateContainerResponse response = docker.getDocker().createContainerCmd(Constants.Images.HTPASSWD.image())
                .withCmd(mainConfig.getRegistryUsername(), mainConfig.getRegistryPassword())
                .exec();

        log.debug("Htpasswd creation container: {}.", response.getId());
        docker.getDocker().startContainerCmd(response.getId()).exec();

        int exitCode = docker.getDocker().waitContainerCmd(response.getId())
                .exec(new WaitContainerResultCallback())
                .awaitStatusCode(10, TimeUnit.SECONDS);

        if (exitCode != 0) {
            throw new FatalDockerMCError("Htpasswd creation container {} exited with code {}.", response.getId(), exitCode);
        }

        // Save the htpasswd
        String containerOutput = docker.getContainerLog(response.getId());

        if (!containerOutput.startsWith(mainConfig.getRegistryUsername())) {
            throw new FatalDockerMCError("Faulty content of htpasswd file: '" + containerOutput + "'");
        }

        File htpasswdFile = new File(Constants.REGISTRY_CERT_PATH_1 + "htpasswd");
        FileUtils.write(htpasswdFile, containerOutput, StandardCharsets.UTF_8);
    }


    /**
     * Utility class to work with the certificates for the registry
     */
    public static class RegistryCertFiles {

        private final File caCrt = new File(Constants.REGISTRY_CERT_PATH_1 + "ca.crt");
        private final File caKey = new File(Constants.REGISTRY_CERT_PATH_1 + "ca.key");
        private final File htpasswd = new File(Constants.REGISTRY_CERT_PATH_1 + "htpasswd");

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
