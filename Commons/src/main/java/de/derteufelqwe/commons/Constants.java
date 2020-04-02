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
    public static String WORKDIR = System.getProperty("user.dir").replace('\\', '/') + "/Server/";

    public static String CONFIG_PATH = WORKDIR + "server/configs/";

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
    public static String REGISTRY_CERT_PATH = WORKDIR + "server/internal/security/registry-certs/";
    public static String REGISTRY_CERT_NAME  = "ca.crt";
    public static String REGISTRY_KEY_NAME  = "ca.key";
    public static String REGISTRY_HTPASSWD_NAME = "htpasswd";
    public static String REGISTRY_URL = "registry.swarm";

    // -----  DNS  -----
    public static String DNS_WORKDIR_PATH = WORKDIR + "server/internal/workdirs/dns/";
    public static boolean DNS_WEBMIN_ENABLED = true;
    public static String DNS_SYSTEM_ENTRY_PATH = DNS_WORKDIR_PATH + "/bind/lib/swarm.entrys_system";
    public static String DNS_USER_ENTRY_PATH = DNS_WORKDIR_PATH + "/bind/lib/swarm.entrys_user";

    // -----  API  -----
    public static String API_CERTS_PATH = WORKDIR + "server/internal/security/api-certs/";
    public static String DOCKER_SOCKET_PATH = "/var/run/docker.sock";
    public static String APIPROXY_CONTAINER_NAME = "APIProxy";

    // -----  Webserver  -----
    public static String WEBSERVER_CONTAINER_NAME = "ConfigServer";
    public static int WEBSERVER_PORT = 8101;

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

    // Values for the Tag CONTAINER_IDENTIFIER_KEY
    public enum ContainerType {
        REGISTRY,
        REGISTRY_CERTS_GEN,
        DNS,
        API_PROXY,
        API_PROXY_CERTS_GEN,
        CONFIG_WEBSERVER,

        BUNGEE,
        MINECRAFT,
        MINECRAFT_POOL
        ;
    }

    public enum Images {
        REGISTRY("registry:2"),
        OPENSSL("frapsoft/openssl:latest"),
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