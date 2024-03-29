package de.derteufelqwe.commons;

import java.nio.charset.StandardCharsets;
import java.sql.Timestamp;
import java.util.Collections;
import java.util.Map;
import java.util.UUID;

public class Constants {

    public static String WORKDIR = System.getProperty("user.dir").replace("\\", "/") + "/";

    // Miscellaneous values
    public static final String CONFIG_PATH = WORKDIR + "configs/";
    public static final String DATA_PATH = WORKDIR + "/internal/data/";
    public static final String DOCKER_DRIVER_PLUGIN_NAME = "derteufelqwe/dockermc-drivers:latest";
    /**
     * This is a key, which can be used to label a docker node
     * Possible values: "true", "false"
     * Indicates that a master node should (not) be responsible for reading nodes and service events
     */
    public static final String NODEWATCHER_MASTER = "NODEWATCHER_MASTER";
    public static final String DB_DMC_USER = "dockermc";
    /**
     * DNS name of the DockerMC master node, which hosts the DB.
     */
    public static final String DMC_MASTER_DNS_NAME = "dmcmaster";
    public static final int ZSTD_COMPRESSION_LEVEL = 10;
    /**
     * See Volume#groupName parameter
     */
    public static final String VOLUME_GROUPNAME_KEY = "GroupName";

    // Networking
    public static final String NETW_OVERNET_NAME = "overnet";
    public static final String SUBNET_OVERNET = "11.0.0.0/8";

    // Docker image building
    public static final String IMAGE_MINECRAFT_PATH = WORKDIR + "images/minecraft/";
    public static final String IMAGE_BUNGEE_PATH = WORKDIR + "images/bungeecord";
    public static final String DOCKERFILE_MINECRAFT_PATH = WORKDIR + "internal/dockerfiles/minecraft.dfile";
    public static final String DOCKERFILE_BUNGEE_PATH = WORKDIR + "internal/dockerfiles/bungee.dfile";
    public static final String DOCKER_IMAGE_TYPE_TAG = "DockerMCImageType";

    // -----  Registry  -----
    public static final String REGISTRY_CONTAINER_NAME = "Registry";
    public static final int REGISTY_CONTAINER_DEFAULT_PORT = 5000;
    // Path to the Registry Certificate and key
    public static final String RAW_REGISTRY_CERT_PATH = "internal/security/registry-certs/";
    public static final String REGISTRY_CERT_PATH = WORKDIR + RAW_REGISTRY_CERT_PATH;
    public static final String REGISTRY_CERT_NAME  = "ca.crt";
    public static final String REGISTRY_KEY_NAME  = "ca.key";
    public static final String REGISTRY_HTPASSWD_NAME = "htpasswd";
    public static final String REGISTRY_URL = "registry.swarm";
    public static final String REGISTRY_VOLUME_NAME = "registry_data";

    // -----  Tags  -----
    // Identifies a container, which belongs the DockerMC
    public static final String DOCKER_IDENTIFIER_KEY = "Owner";
    public static final String DOCKER_IDENTIFIER_VALUE = "DockerMC";
    // Name, which the container should have in Minecraft
    public static final String SERVER_NAME_KEY = "ServerName";
    // Tag Key to identify types of Containers
    public static final String CONTAINER_IDENTIFIER_KEY = "Type";
    public static final Map<String, String> DOCKER_IDENTIFIER_MAP = Collections.singletonMap(DOCKER_IDENTIFIER_KEY, DOCKER_IDENTIFIER_VALUE);

    // -----  Timings  -----
    public static final int LOG_FETCH_TIME = 20;

    // -----  Postgres  -----
    public static final String POSTGRESDB_CONTAINER_NAME = "Postgres";
    public static final int POSTGRESDB_PORT = 5432;
    public static final String POSTGRES_VOLUME_NAME = "dmc_postgres_data";
    public static final UUID CONSOLE_USER_UUID = new UUID(Long.MAX_VALUE, Long.MAX_VALUE);
    public static final String CONSOLE_USER_NAME = "Console-Website-User";

    // -----  Redis  -----
    public static final String REDIS_CONTAINER_NAME = "Redis";
    public static final int REDIS_PORT = 6379;
    public static final byte[] REDIS_MESSAGES_CHANNEL = "messages".getBytes(StandardCharsets.UTF_8);

    // -----  Redis keys  -----
    public static final String REDIS_KEY_PLAYERCOUNT = "playerCount";
    public static final String REDIS_KEY_LOBBYSERVER = "system#lobbyServerName";

    // BungeeCord
    public static final Timestamp BAN_PERMANENT_TIMESTAMP = new Timestamp(4102444800000L);

    // Values for the Tag CONTAINER_IDENTIFIER_KEY
    public enum ContainerType {
        REGISTRY,
        REGISTRY_CERTS_GEN,
        OVERNET,    // Not actually a container
        NODE_WATCHER,
        NODE_WATCHER_POOL,
        POSTGRES_DB,
        REDIS_DB,

        BUNGEE,
        BUNGEE_POOL,
        MINECRAFT,
        MINECRAFT_POOL,
        MINECRAFT_PERSISTENT,
        MINECRAFT_POOL_PERSISTENT
        ;
    }

    /**
     * Important!
     *      Image names must contain a version tag like 'latest' or docker will download every version of the image.
     */
    public enum Images {
        REGISTRY("registry:latest"),
        OPENSSL("frapsoft/openssl:latest"),
        HTPASSWD("xmartlabs/htpasswd:latest"),
        POSTGRES("postgres:latest"),
        REDIS("redis:latest"),
        DECKSCHRUBBER("lhanxetus/deckschrubber:latest"),
        NODEWATCHER("registry.swarm/nodewatcher:latest")
        ;

        private String imageName;

        Images(String name) {
            this.imageName = name;
        }

        public String image() {
            return this.imageName;
        }
    }

}
