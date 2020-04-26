package de.derteufelqwe.ServerManager.commands.system;

import com.github.dockerjava.api.command.CreateContainerResponse;
import com.github.dockerjava.api.model.Bind;
import com.github.dockerjava.api.model.Volume;
import com.github.dockerjava.core.command.WaitContainerResultCallback;
import de.derteufelqwe.commons.Constants;
import de.derteufelqwe.ServerManager.Docker;
import de.derteufelqwe.ServerManager.ServerManager;
import de.derteufelqwe.ServerManager.Utils;
import de.derteufelqwe.ServerManager.config.backend.Config;
import de.derteufelqwe.ServerManager.config.MainConfig;
import de.derteufelqwe.ServerManager.config.objects.CertificateCfg;
import de.derteufelqwe.ServerManager.exceptions.FatalDockerMCError;
import org.apache.commons.io.FileUtils;
import picocli.CommandLine;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

/**
 * List the Certificates
 */
@CommandLine.Command(name = "gen", description = "Generate the required certificates.",
        mixinStandardHelpOptions = true, subcommands = {
})
public class CertificatesGen implements Runnable {

    private final Pattern EXIT_CODE_CAPTURE_REGEX = Pattern.compile("(?<=Exited \\()(.*?)(?=\\)).*");

    private Docker docker;
    private final int DURATION = 365;   // days
    private Map<String, String> labels; // The labels for this container


    // ToDo: Change to false
    @CommandLine.Option(names = {"-o", "--overwrite"}, description = "Overwrite existing certificates.")
    private boolean doOverwrite = true;

    public CertificatesGen() {
        this.docker = ServerManager.getDocker();
        this.labels = Utils.quickLabel(Constants.ContainerType.REGISTRY_CERTS_GEN);
    }


    /**
     * Check if a certificate got created already.
     */
    private boolean checkForExistingCert() {
        File certsPath = new File(Constants.REGISTRY_CERT_PATH);
        certsPath.mkdirs();

        if (!certsPath.exists()) {
            throw new FatalDockerMCError("Failed to create path " + certsPath.getAbsolutePath());
        }

        if (certsPath.list() != null && Arrays.asList(certsPath.list()).contains(Constants.REGISTRY_CERT_NAME)) {
            return true;
        }

        return false;
    }


    /**
     * Generate the SSL certificate. No failure checks.
     *
     * Example command: req -newkey rsa:4096 -nodes -sha256 -keyout /export/ca.key -x509 -days 365
     * -out /export/ca.crt -subj "/C=US/ST=Denial/L=Springfield/O=Dis/CN=registry.swarm"
     */
    private String generateSSLCerts() {
        CertificateCfg cfg = Config.get(MainConfig.class).getRegistryCerCfg();
        Volume sslOutput = new Volume("/export");
        List<String> command = new ArrayList<>(Arrays.asList(
                "req", "-newkey", "rsa:4096", "-nodes", "-sha256", "-x509", "-days", Integer.toString(DURATION),
                "-out", "/export/" + Constants.REGISTRY_CERT_NAME, "-keyout", "/export/" + Constants.REGISTRY_KEY_NAME,
                "-subj"));

        command.add(String.format("/C=%s/ST=%s/L=%s/O=%s/CN=%s/emailAddress=%s",
                cfg.getCountryCode(), cfg.getState(), cfg.getCity(), cfg.getOrganizationName(), Constants.REGISTRY_URL, cfg.getEmail()));


        CreateContainerResponse response = docker.getDocker().createContainerCmd(Constants.Images.OPENSSL.image())
                .withLabels(labels)
                .withVolumes(sslOutput)
                .withBinds(new Bind(Constants.REGISTRY_CERT_PATH, sslOutput))
                .withCmd(command)
                .exec();

        docker.getDocker().startContainerCmd(response.getId()).exec();

        return response.getId();
    }


    /**
     * Checks if the container correctly finished
     * @param containerID ContainerID from {@link #generateSSLCerts()}
     * @return Successful (true) / failed (false)
     */
    private boolean checkForErrorsSSL(String containerID) {
        int exitCode = docker.getDocker().waitContainerCmd(containerID)
                .exec(new WaitContainerResultCallback())
                .awaitStatusCode(10, TimeUnit.SECONDS);

        return exitCode == 0;
    }


    /**
     * Generates a htpasswd file, which contains the password for the registry.
     * @return The containerID
     */
    private String generateHtpasswd() {
        MainConfig cfg = Config.get(MainConfig.class);
        List<String> cmd = Arrays.asList("-Bbn", cfg.getRegistryUsername(), cfg.getRegistryPassword());

//        docker.pullImage(Constants.Images.REGISTRY.name());

        CreateContainerResponse container = docker.getDocker().createContainerCmd(Constants.Images.REGISTRY.image())
                .withLabels(labels)
                .withEntrypoint("htpasswd")
                .withCmd(cmd)
                .exec();

        docker.getDocker().startContainerCmd(container.getId()).exec();

        return container.getId();
    }


    /**
     * Saves the file and checks if the generated htpasswd file got generated correctly.
     * @param containerID ContainerID, where the file got generated in.
     * @return
     */
    private boolean saveAndcheckForErrorsHTP(String containerID) {
        int statusCode = docker.getDocker().waitContainerCmd(containerID)
                .exec(new WaitContainerResultCallback())
                .awaitStatusCode(10, TimeUnit.SECONDS);

        String fileContent = docker.getContainerLog(containerID);

        File file = new File(Constants.REGISTRY_CERT_PATH + Constants.REGISTRY_HTPASSWD_NAME);
        try {
            FileUtils.writeStringToFile(file, fileContent, StandardCharsets.UTF_8);
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }

        return statusCode == 0;
    }


    @Override
    public void run() {
        if (!doOverwrite) {
            boolean certExisting = this.checkForExistingCert();
            if (certExisting) {
                System.out.println("A certificate was already found. Use -o if you want to overwrite it.");
                return;
            }
        }

        String containerID = generateSSLCerts();
        System.out.println("SSLGeneration-Container: " + containerID);

        if (!this.checkForErrorsSSL(containerID)) {
            String logs = docker.getContainerLog(containerID);
            System.err.println("Failed to create certificate.");
            System.err.println(logs);
            return;
        } else {
            System.out.println("Successfully generated an SSL certificate.");
        }

        String htpContainerID = generateHtpasswd();
        System.out.println("HTPasswdGeneration-Container: " + htpContainerID);

        if (!this.saveAndcheckForErrorsHTP(htpContainerID)) {
            System.err.println("Container " + htpContainerID + " failed to generate htpasswd file.");
            return;
        } else {
            System.out.println("Successfully generated htpasswd file.");
        }

    }

}
