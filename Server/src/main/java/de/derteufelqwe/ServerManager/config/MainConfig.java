package de.derteufelqwe.ServerManager.config;

import de.derteufelqwe.ServerManager.config.objects.CertificateCfg;
import de.derteufelqwe.commons.config.annotations.Comment;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class MainConfig {

    @Comment("IP for the Docker-Daemon")
    private String dockerIP = "localhost";
    @Comment("Port of the Docker-daemon. 2375 for insecure and 2376 for TLS.")
    private int dockerPort = 2375;
    @Comment("Protocol to connect to Docker-daemon. Dont' change!")
    private String dockerProtocol = "tcp";
    @Comment("Set to false for insecure debug mode.")
    private boolean useTLSVerify = false;
    @Comment("Docker-API version")
    private String APIVersion = "1.40";

    // ToDo: Change back to private when kotlin works together with lombok
    @Comment("Username for the registry")
    public String registryUsername = "admin";
    @Comment("Password for the registry")
    public String registryPassword = "root";

    @Comment("Configuration for the registry certificate")
    private CertificateCfg registryCerCfg = new CertificateCfg();

    @Comment("Amount of BungeeCord-Pool tasks, that can get updated in parallel. This should be less than your maximum instances.")
    private int bungeePoolParallelUpdates = 2;
    @Comment("Amount of Looby-Pool tasks, that can get updated in parallel. This should be less than your maximum instances.")
    private int lobbyPoolParallelUpdates = 2;
    @Comment("Amount of Pool tasks, that can get updated in parallel. This should be less than your maximum instances.")
    private int poolParallelUpdates = 2;

    @Comment("Forces DMC to stop the containers before starting a new one on a service update.")
    private boolean forceStopFirst = false;

    @Comment("The IP of the server, which acts as the DockerMC master node")
    private String dockerMasterIP = "";
}


