package de.derteufelqwe.ServerManager.setup;

import com.github.dockerjava.api.command.CreateContainerResponse;
import com.github.dockerjava.api.command.WaitContainerResultCallback;
import com.github.dockerjava.api.model.Bind;
import com.github.dockerjava.api.model.Volume;
import de.derteufelqwe.commons.Constants;
import de.derteufelqwe.ServerManager.Docker;
import de.derteufelqwe.ServerManager.ServerManager;
import de.derteufelqwe.ServerManager.Utils;
import de.derteufelqwe.ServerManager.config.Config;
import de.derteufelqwe.ServerManager.config.configs.MainConfig;
import de.derteufelqwe.ServerManager.config.configs.objects.CertificateCfg;
import de.derteufelqwe.ServerManager.setup.objects.APIProxyCertFiles;
import de.derteufelqwe.ServerManager.setup.objects.RegistryCertFiles;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Utility class to handle generation of certificates
 */
public class CertificateCreator {

    private Docker docker;


    public CertificateCreator() {
        this.docker = ServerManager.getDocker();
    }


    /**
     * Checks if the certs do exist and if they are incomplete, deletes all of them
     *
     * @return
     */
    public boolean findAPIProxyCerts() {
        APIProxyCertFiles apiProxyCertFiles = new APIProxyCertFiles();
        boolean exist = apiProxyCertFiles.filesExist();

        if (exist) {
            return true;

        } else {
            apiProxyCertFiles.deleteFiles();
            return false;
        }
    }

    /**
     * Generates the required certificates for the API-Proxy
     * @param forceRegeneration Delete existing certificates before generating new ones
     */
    public String generateAPIProxyCerts(boolean forceRegeneration) {
        if (forceRegeneration) {
            System.out.println("Regenerating API-Proxy certificates...");
            new APIProxyCertFiles().deleteFiles();

        } else {
            System.out.println("Regenerating API-Proxy certificates...");
        }

        List<Bind> binds = Arrays.asList(
                new Bind(Constants.API_CERTS_PATH, new Volume("/data/certs"))
        );

        List<String> envs = Arrays.asList("CREATE_CERTS_WITH_PW=root", "CERT_HOSTNAME=" + Constants.APIPROXY_CONTAINER_NAME);


        CreateContainerResponse response = docker.getDocker().createContainerCmd(Constants.Images.API_PROXY.image())
                .withLabels(Utils.quickLabel(Constants.ContainerType.API_PROXY_CERTS_GEN))
                .withBinds(binds)
                .withEnv(envs)
                .withCmd("hostname")    // To make the container exit after generation
                .exec();


        docker.getDocker().startContainerCmd(response.getId()).exec();

        docker.getDocker().waitContainerCmd(response.getId())
                .exec(new WaitContainerResultCallback())
                .awaitStatusCode(10, TimeUnit.SECONDS);

        System.out.println("Generated certificates.");

        return response.getId();
    }



    /**
     * Checks if the certificates exist already. If they are incomplete all files get deleted.
     *
     * @return
     */
    public boolean findRegistryCerts() {
        RegistryCertFiles registryCertFiles = new RegistryCertFiles();
        boolean exist = registryCertFiles.filesExist();

        if (exist) {
            return true;

        } else {
            registryCertFiles.deleteFiles();
            return false;
        }

    }

    /**
     * Generates the required certificates for the registry.
     * This includes the htpasswd file.
     * @param forceRegeneration Delete old certificates before generating new ones
     */
    public String generateRegistryCerts(boolean forceRegeneration) {
        MainConfig mainConfig = Config.get(MainConfig.class);
        CertificateCfg cfg = mainConfig.getRegistryCerCfg();

        if (forceRegeneration) {
            new RegistryCertFiles().deleteFiles();
        }

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

        return response.getId();
    }

}
