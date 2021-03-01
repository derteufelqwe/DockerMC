package de.derteufelqwe.commons;

import java.nio.charset.StandardCharsets;
import java.sql.Timestamp;
import java.util.UUID;

public class Constants {


    public static final String LOGO =
            " _____                         ___  ___                                  \n" +
            "/  ___|                        |  \\/  |                                  \n" +
            "\\ `--.  ___ _ ____   _____ _ __| .  . | __ _ _ __   __ _  __ _  ___ _ __ \n" +
            " `--. \\/ _ \\ '__\\ \\ / / _ \\ '__| |\\/| |/ _` | '_ \\ / _` |/ _` |/ _ \\ '__|\n" +
            "/\\__/ /  __/ |   \\ V /  __/ |  | |  | | (_| | | | | (_| | (_| |  __/ |   \n" +
            "\\____/ \\___|_|    \\_/ \\___|_|  \\_|  |_/\\__,_|_| |_|\\__,_|\\__, |\\___|_|   \n" +
            "                                                          __/ |          \n" +
            "                                                         |___/           ";

    public static final String AUTHOR = "derteufelqwe";
    public static final String WORKDIR_WND = "C:/Users/Arne/Desktop/ServerManager/Server/";
    public static final String WORKDIR = System.getProperty("user.dir").replace("\\", "/") + "/";

    public static final String CONFIG_PATH = WORKDIR + "server/configs/";

    // Networking
    public static final String NETW_OVERNET_NAME = "overnet";
    public static final String SUBNET_OVERNET = "11.0.0.0/8";

    // Docker image building
    public static final String IMAGE_PATH = WORKDIR + "server/images/";
    public static final String DOCKERFILES_PATH = WORKDIR + "server/internal/dockerfiles/";

    // -----  Registry  -----
    // Path to the Registry Certificate and key
    private static final String registryCertPath = "server/internal/security/registry-certs/";

    /**
     * @param windows If true, returns the path required for the windows host, if false for docker
     */
    public static final String REGISTRY_CERT_PATH(boolean windows) { return windows ? WORKDIR_WND + registryCertPath : WORKDIR + registryCertPath; }
    public static final String REGISTRY_CERT_NAME  = "ca.crt";
    public static final String REGISTRY_KEY_NAME  = "ca.key";
    public static final String REGISTRY_HTPASSWD_NAME = "htpasswd";
    public static final String REGISTRY_URL = "registry.swarm";

    // -----  Tags  -----
    // Identifies a container, which belongs the DockerMC
    public static final String DOCKER_IDENTIFIER_KEY = "Owner";
    public static final String DOCKER_IDENTIFIER_VALUE = "DockerMC";
    // Name, which the container should have in Minecraft
    public static final String SERVER_NAME_KEY = "ServerName";
    // Tag Key to identify types of Containers
    public static final String CONTAINER_IDENTIFIER_KEY = "Type";

    // -----  Timings  -----
    // Time in seconds which containers have to get up and running
    public static final int CONTAINER_STARTUP_TIME = 10;
    public static final int SERVICE_STARTUP_TIME = 20;
    public static final int LOG_FETCH_TIME = 20;

    // -----  Postgres  -----
    public static final String POSTGRESDB_CONTAINER_NAME = "Postgres";
    public static final int POSTGRESDB_PORT = 5432;
    public static final UUID CONSOLE_USER_UUID = new UUID(Long.MAX_VALUE, Long.MAX_VALUE);
    public static final String CONSOLE_USER_NAME = "Console-Website-User";

    // -----  Redis  -----
    public static final String REDIS_CONTAINER_NAME = "Redis";
    public static final int REDIS_PORT = 6379;
    public static final byte[] REDIS_MESSAGES_CHANNEL = "messages".getBytes(StandardCharsets.UTF_8);

    // -----  Redis keys  -----
    public static final String REDIS_KEY_PLAYERCOUNT = "playerCount";
    public static final String REDIS_KEY_LOBBYSERVER = "system#lobbyServerName";

    // -----  Consul  -----
    public static final String CONSUL_HOST = "Consul";
    public static final int CONSUL_PORT = 8500;

    // BungeeCord
    public static final Timestamp BAN_PERMANENT_TIMESTAMP = new Timestamp(4102444800000L);

    // Values for the Tag CONTAINER_IDENTIFIER_KEY
    public enum ContainerType {
        REGISTRY,
        REGISTRY_CERTS_GEN,
        OVERNET,    // Not actually a container
        CONSUL,
        CONSUL_POOL,
        LOGCOLLECTOR,
        LOGCOLLECTOR_POOL,
        POSTGRES_DB,
        REDIS_DB,

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

    /**
     * Important!
     *      Image names must container a version tag like 'latest' or docker will download every version of the image.
     */
    public enum Images {
        REGISTRY("registry:latest"),
        OPENSSL("frapsoft/openssl:latest"),
        HTPASSWD("xmartlabs/htpasswd:latest"),
        CONSUL("consul:latest"),
        POSTGRES("postgres:latest"),
        LOGCOLLECTOR("registry.swarm/logcollector"),
        REDIS("redis:latest")
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
        SYSTEM("SystemData.yml")
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
