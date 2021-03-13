package de.derteufelqwe.ServerManager.config;

import de.derteufelqwe.ServerManager.config.objects.CertificateCfg;
import de.derteufelqwe.commons.config.annotations.Comment;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class MainConfig {

    @Comment("IP for the Docker-Daemon")
    private String dockerIP = "localhost";  // host.docker.internal to access the docker API
    @Comment("Port of the Docker-daemon. 2375 for insecure and 2376 for TLS.")
    private int dockerPort = 2375;
    @Comment("Protocol to connect to Docker-daemon. Dont' change!")
    private String dockerProtocol = "tcp";
    @Comment("Set to false for insecure debug mode.")
    private boolean useTLSVerify = false;
    @Comment("Docker-API version")
    private String APIVersion = "1.40";

    private int proxyPort = 25565;

    @Comment("Username for the registry")
    private String registryUsername = "admin";
    @Comment("Password for the registry")
    private String registryPassword = "root";

    @Comment("Configuration for the registry certificate")
    private CertificateCfg registryCerCfg = new CertificateCfg();

    @Comment("Amount of BungeeCord-Pool tasks, that can get updated in parallel. This should be less than your maximum instances.")
    private int bungeePoolParallelUpdates = 2;
    @Comment("Amount of Looby-Pool tasks, that can get updated in parallel. This should be less than your maximum instances.")
    private int lobbyPoolParallelUpdates = 2;
    @Comment("Amount of Pool tasks, that can get updated in parallel. This should be less than your maximum instances.")
    private int PoolParallelUpdates = 2;

    @Comment("Forces DMC to stop the containers before starting a new one on a service update.")
    private boolean forceStopFirst = false;
}


