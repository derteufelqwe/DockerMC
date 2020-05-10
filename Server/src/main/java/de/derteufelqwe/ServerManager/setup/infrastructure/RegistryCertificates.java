package de.derteufelqwe.ServerManager.setup.infrastructure;

import com.github.dockerjava.api.command.CreateContainerResponse;
import com.github.dockerjava.api.command.WaitContainerResultCallback;
import com.github.dockerjava.api.model.Bind;
import com.github.dockerjava.api.model.Volume;
import de.derteufelqwe.ServerManager.Docker;
import de.derteufelqwe.ServerManager.Utils;
import de.derteufelqwe.ServerManager.config.MainConfig;
import de.derteufelqwe.ServerManager.config.backend.Config;
import de.derteufelqwe.ServerManager.config.objects.CertificateCfg;
import de.derteufelqwe.ServerManager.setup.DockerObjTemplate;
import de.derteufelqwe.commons.Constants;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class RegistryCertificates {

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
        MainConfig mainConfig = Config.get(MainConfig.class);
        CertificateCfg cfg = mainConfig.getRegistryCerCfg();


        // -----  SSL-certificate generation  -----

        Volume sslOutput = new Volume("/export");
        List<String> command = new ArrayList<>(Arrays.asList(
                "req", "-newkey", "rsa:4096", "-nodes", "-sha256", "-x509", "-days", "356",
                "-out", "/export/" + Constants.REGISTRY_CERT_NAME, "-keyout", "/export/" + Constants.REGISTRY_KEY_NAME,
                "-subj"));

        command.add(String.format("/C=%s/ST=%s/L=%s/O=%s/CN=%s/emailAddress=%s",
                cfg.getCountryCode(), cfg.getState(), cfg.getCity(), cfg.getOrganizationName(), Constants.REGISTRY_URL, cfg.getEmail()));


        CreateContainerResponse response = docker.getDocker().createContainerCmd(Constants.Images.OPENSSL.image())
                .withLabels(Utils.quickLabel(Constants.ContainerType.REGISTRY_CERTS_GEN))
                .withVolumes(sslOutput)
                .withBinds(new Bind(Constants.REGISTRY_CERT_PATH, sslOutput))
                .withCmd(command)
                .exec();

        docker.getDocker().startContainerCmd(response.getId()).exec();

        docker.getDocker().waitContainerCmd(response.getId())
                .exec(new WaitContainerResultCallback())
                .awaitStatusCode(10, TimeUnit.SECONDS);

        // -----  Generate htpasswd file  -----

        CreateContainerResponse htpasswdContainer = docker.getDocker().createContainerCmd(Constants.Images.REGISTRY.image())
                .withEntrypoint("htpasswd")
                .withCmd("-Bbn", mainConfig.getRegistryUsername(), mainConfig.getRegistryPassword())
                .exec();

        docker.getDocker().startContainerCmd(htpasswdContainer.getId()).exec();

        docker.getDocker().waitContainerCmd(htpasswdContainer.getId())
                .exec(new WaitContainerResultCallback())
                .awaitStatusCode(10, TimeUnit.SECONDS);

        String containerOutput = docker.getContainerLog(htpasswdContainer.getId());

        File htpasswdFile = new File(Constants.REGISTRY_CERT_PATH + "htpasswd");

        try {
            FileWriter writer = new FileWriter(htpasswdFile);
            writer.write(containerOutput);
            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

        return new DockerObjTemplate.CreateResponse(true, response.getId());
    }


    public DockerObjTemplate.DestroyResponse destroy() {
        RegistryCertFiles registryCertFiles = new RegistryCertFiles();
        boolean exists = registryCertFiles.filesExist();
        registryCertFiles.deleteFiles();

        return new DockerObjTemplate.DestroyResponse(exists, null);
    }


    /**
     * Utility class to work with the certificates for the registry
     */
    public class RegistryCertFiles {

        private File caCrt = new File(Constants.REGISTRY_CERT_PATH + "ca.crt");
        private File caKey = new File(Constants.REGISTRY_CERT_PATH + "ca.key");
        private File htpasswd = new File(Constants.REGISTRY_CERT_PATH + "htpasswd");

        public RegistryCertFiles() {
        }

        public boolean filesExist() {
            return caCrt.exists() && caKey.exists() && htpasswd.exists();
        }

        public void deleteFiles() {
            caCrt.delete();
            caKey.delete();
            htpasswd.delete();
        }

    }

}
