package de.derteufelqwe.commons;

public class Constants {


    public static String LOGO =
            " _____                         ___  ___                                  \n" +
            "/  ___|                        |  \\/  |                                  \n" +
            "\\ `--.  ___ _ ____   _____ _ __| .  . | __ _ _ __   __ _  __ _  ___ _ __ \n" +
            " `--. \\/ _ \\ '__\\ \\ / / _ \\ '__| |\\/| |/ _` | '_ \\ / _` |/ _` |/ _ \\ '__|\n" +
            "/\\__/ /  __/ |   \\ V /  __/ |  | |  | | (_| | | | | (_| | (_| |  __/ |   \n" +
            "\\____/ \\___|_|    \\_/ \\___|_|  \\_|  |_/\\__,_|_| |_|\\__,_|\\__, |\\___|_|   \n" +
            "                                                          __/ |          \n" +
            "                                                         |___/           ";

    public static String AUTHOR = "derteufelqwe";
    public static String WORKDIR_WND = "C:/Users/Arne/Desktop/ServerManager/Server/";
    public static String WORKDIR = "/home/arne/ServerManager/Server/";

    public static String CONFIG_PATH = WORKDIR_WND + "server/configs/";

    // Networking
    public static String NETW_OVERNET_NAME = "overnet";
    public static String NETW_API_NAME = "api_net";
    public static String SUBNET_OVERNET = "11.0.0.0/8";
    public static String SUBNET_APINET = "12.1.0.0/16";

    // Docker image building
    public static String IMAGE_PATH = WORKDIR + "server/images/";
    public static String DOCKERFILES_PATH = WORKDIR + "server/internal/dockerfiles/";

    // -----  Registry  -----
    // Path to the Registry Certificate and key
    private static String registryCertPath = "server/internal/security/registry-certs/";

    /**
     * @param windows If true, returns the path required for the windows host, if false for docker
     */
    public static String REGISTRY_CERT_PATH(boolean windows) { return windows ? WORKDIR_WND + registryCertPath : WORKDIR + registryCertPath; }
    public static String REGISTRY_CERT_NAME  = "ca.crt";
    public static String REGISTRY_KEY_NAME  = "ca.key";
    public static String REGISTRY_HTPASSWD_NAME = "htpasswd";
    public static String REGISTRY_URL = "registry.swarm";

    // -----  Tags  -----
    // Identifies a container, which belongs the DockerMC
    public static String DOCKER_IDENTIFIER_KEY = "Owner";
    public static String DOCKER_IDENTIFIER_VALUE = "DockerMC";
    // Name, which the container should have in Minecraft
    public static String SERVER_NAME_KEY = "ServerName";
    // Tag Key to identify types of Containers
    public static String CONTAINER_IDENTIFIER_KEY = "Type";

    // -----  Timings  -----
    // Time in seconds which containers have to get up and running
    public static int CONTAINER_STARTUP_TIME = 10;
    public static int SERVICE_STARTUP_TIME = 20;
    public static int LOG_FETCH_TIME = 20;

    // -----  Consul  -----
    public static String CONSUL_HOST = "Consul";
    public static int CONSUL_PORT = 8500;

    // Values for the Tag CONTAINER_IDENTIFIER_KEY
    public enum ContainerType {
        REGISTRY,
        REGISTRY_CERTS_GEN,
        DNS,
        API_PROXY,
        API_PROXY_CERTS_GEN,
        CONSUL,
        CONSUL_POOL,

        NGINX,
        NGINX_POOL,
        BUNGEE,
        BUNGEE_POOL,
        MINECRAFT,
        MINECRAFT_PERSISTENT,
        MINECRAFT_POOL,
        MINECRAFT_POOL_PERSISTENT
        ;
    }

    public enum Images {
        REGISTRY("registry:latest"),
        OPENSSL("frapsoft/openssl:latest"),
        HTPASSWD("xmartlabs/htpasswd:latest"),
        BINDDNS("sameersbn/bind:latest"),
        API_PROXY("derteufelqwe/docker-api-proxy:latest"),
        CONFIG_WEBSERVER("configwebserver")
        ;

        private String imageName;

        Images(String name) {
            this.imageName = name;
        }

        public String image() {
            return this.imageName;
        }
    }

    public enum Configs {
        MAIN("MainConfig.yml"),
        INFRASTRUCTURE("InfrastructureConfig.yml"),
        RUNNING("RunningConfig.yml")
        ;

        private String fileName;

        Configs(String name) {
            this.fileName = name;
        }

        public String filename() {
            return this.fileName;
        }

    }

}
