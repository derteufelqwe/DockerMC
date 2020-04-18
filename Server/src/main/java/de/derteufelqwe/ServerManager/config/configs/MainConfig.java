package de.derteufelqwe.ServerManager.config.configs;

import com.google.gson.annotations.Expose;
import de.derteufelqwe.ServerManager.config.Ignore;
import de.derteufelqwe.ServerManager.config.YAMLComment;
import de.derteufelqwe.ServerManager.config.configs.objects.CertificateCfg;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class MainConfig {

    @YAMLComment("IP for the Docker-Daemon")
    private String dockerIP = "localhost";  // host.docker.internal to access the docker API
    @YAMLComment("Port of the Docker-daemon. 2375 for insecure and 2376 for TLS.")
    private int dockerPort = 2375;
    @YAMLComment("Protocol to connect to Docker-daemon. Dont' change!")
    private String dockerProtocol = "tcp";
    @YAMLComment("Set to false for insecure debug mode.")
    private boolean useTLSVerify = false;
    @YAMLComment("Docker-API version")
    private String APIVersion = "1.40";

    private int minecraftPort = 25565;
    private int proxyPort = 25565;

    @YAMLComment("Tag name to identify all docker containers, which belong to DockerMC.")
    private String idTag = "DockerMC";
    @YAMLComment("Tag to identify all Bungeecord proxies.")
    private String proxyID = "bungee";
    @YAMLComment("Tag to identify all Minecraft servers")
    private String mcServerID = "mcServer";

    @YAMLComment("Username for the registry")
    private String registryUsername = "admin";
    @YAMLComment("Password for the registry")
    private String registryPassword = "root";

    @YAMLComment("Configuration for the registry certificate")
    private CertificateCfg registryCerCfg = new CertificateCfg();

}

