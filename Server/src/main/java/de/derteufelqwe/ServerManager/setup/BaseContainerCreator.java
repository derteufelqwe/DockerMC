package de.derteufelqwe.ServerManager.setup;

import com.github.dockerjava.api.command.CreateContainerResponse;
import com.github.dockerjava.api.command.CreateNetworkResponse;
import com.github.dockerjava.api.command.InspectContainerResponse;
import com.github.dockerjava.api.command.WaitContainerResultCallback;
import com.github.dockerjava.api.exception.NotFoundException;
import com.github.dockerjava.api.model.*;
import de.derteufelqwe.ServerManager.Docker;
import de.derteufelqwe.ServerManager.ServerManager;
import de.derteufelqwe.ServerManager.Utils;
import de.derteufelqwe.ServerManager.exceptions.FatalDockerMCError;
import de.derteufelqwe.commons.Constants;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class BaseContainerCreator {

    private final int CONTAINER_START_DELAY = 10;   // Time for containers to get up and running.
    private final int NETWORK_CREATE_DELAY = 2;     // Time for networks to get up and running

    private Docker docker;


    public BaseContainerCreator() {
        docker = ServerManager.getDocker();
    }


    /**
     * Tries to find the DNS container.
     *
     * @return Found (true) or not found (false)
     */
    public boolean findDns() {
        List<Container> dnsContainers = docker.getDocker().listContainersCmd()
                .withLabelFilter(Utils.quickLabel(Constants.ContainerType.DNS))
                .exec();

        if (dnsContainers.size() > 1) {
            throw new FatalDockerMCError("Found multiple DNS containers.");

        } else if (dnsContainers.size() == 1) {
            return true;

        } else {
            return false;
        }
    }

    /**
     * Creates the DNS container and confirms that it's up and running
     *
     * @param removeOldData Overwrite previous DNS settings
     * @return Successfully created (true) or failed (false)
     */
    public boolean createDns(boolean removeOldData) {
        String containerID = this.createDnsContainer(removeOldData);
        try {
            docker.getDocker().waitContainerCmd(containerID)
                    .exec(new WaitContainerResultCallback())
                    .awaitStarted(CONTAINER_START_DELAY, TimeUnit.SECONDS);

            TimeUnit.SECONDS.sleep(1);
            this.setupDNSContainer(containerID);
            TimeUnit.SECONDS.sleep(1);

            InspectContainerResponse r = docker.getDocker().inspectContainerCmd(containerID)
                    .exec();

            return r.getState().getRunning() == null ? false : r.getState().getRunning();

        } catch (InterruptedException e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Creates a DNS container with no checks if it's actually running
     *
     * @return ContainerID
     */
    private String createDnsContainer(boolean deleteOldData) {
        // -----  Delete old data  -----
        if (deleteOldData) {
            File bindFolder = new File(Constants.WORKDIR + Constants.DNS_WORKDIR_PATH + "bind");
            File webminFolder = new File(Constants.WORKDIR + Constants.DNS_WORKDIR_PATH + "webmin");

            bindFolder.delete();
            webminFolder.delete();
        }

        // -----  Setup for the container creation  -----
        List<PortBinding> portBindings = Arrays.asList(
                new PortBinding(Ports.Binding.bindPort(53), ExposedPort.tcp(53)),
                new PortBinding(Ports.Binding.bindPort(53), ExposedPort.udp(53)),
                new PortBinding(Ports.Binding.bindPort(10000), ExposedPort.tcp(10000))
        );

        List<Bind> binds = Arrays.asList(
                new Bind(Constants.DNS_WORKDIR_PATH,
                        new Volume("/data"))
        );

        CreateContainerResponse response = docker.getDocker().createContainerCmd(Constants.Images.BINDDNS.image())
                .withLabels(Utils.quickLabel(Constants.ContainerType.DNS))
                .withEnv("WEBMIN_ENABLED=" + Constants.DNS_WEBMIN_ENABLED)
                .withPortBindings(portBindings)
                .withBinds(binds)
                .exec();

        docker.getDocker().startContainerCmd(response.getId()).exec();

        System.out.println("Created DNS " + response.getId() + ".");

        return response.getId();
    }

    /**
     * Sets the DNS Server up. Configures the zone and adds the first DNS entrys
     */
    private void setupDNSContainer(String containerID) {

        ClassLoader classLoader = getClass().getClassLoader();
        // lib files
        File srcBindLibConfig = new File(classLoader.getResource("templates/bind/lib").getFile());
        File destBindLibConfig = new File(Constants.DNS_WORKDIR_PATH + "/bind/lib");

        InputStream srcSwarmEntrysSystem = classLoader.getResourceAsStream("templates/bind/lib/swarm.entrys_system");
        File destSwarmEntrysSystem = new File(Constants.DNS_WORKDIR_PATH + "bind/lib/swarm.entrys_system");
        InputStream srcSwarmEntrysUser = classLoader.getResourceAsStream("templates/bind/lib/swarm.entrys_user");
        File destSwarmEntrysUser = new File(Constants.DNS_WORKDIR_PATH + "bind/lib/swarm.entrys_user");
        InputStream srcSwarmHosts = classLoader.getResourceAsStream("templates/bind/lib/swarm.hosts");
        File destSwarmHosts = new File(Constants.DNS_WORKDIR_PATH + "bind/lib/swarm.hosts");


        // etc files
        InputStream srcNamedConfLocal = classLoader.getResourceAsStream("templates/bind/etc/named.conf.local");
        File destNamedConfLocal = new File(Constants.DNS_WORKDIR_PATH + "/bind/etc/named.conf.local");
        InputStream srcNamedConfOptions = classLoader.getResourceAsStream("templates/bind/etc/named.conf.options");
        File destNamedConfOptions = new File(Constants.DNS_WORKDIR_PATH + "/bind/etc/named.conf.options");


        try {
//            FileUtils.copyDirectory(srcBindLibConfig, destBindLibConfig);
//            FileUtils.copyFile(srcNamedConfLocal, destNamedConfLocal);
//            FileUtils.copyFile(srcNamedConfOptions, destNamedConfOptions);
            FileUtils.copyInputStreamToFile(srcSwarmEntrysSystem, destSwarmEntrysSystem);
            FileUtils.copyInputStreamToFile(srcSwarmEntrysUser, destSwarmEntrysUser);
            FileUtils.copyInputStreamToFile(srcSwarmHosts, destSwarmHosts);
            FileUtils.copyInputStreamToFile(srcNamedConfLocal, destNamedConfLocal);
            FileUtils.copyInputStreamToFile(srcNamedConfOptions, destNamedConfOptions);

        } catch (IOException e) {
            e.printStackTrace();
        }

        try {
            TimeUnit.SECONDS.sleep(5);  // Muss > 1 sein, da der Reload sonst fehlschlägt
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        // ToDo: DNS umbauen, dass die Dateien erst kopiert werden und der Container dann startet

        docker.execContainer(containerID, "/etc/init.d/bind9", "force-reload");

        System.out.println("Configured an reloaded DNS server.");
    }


    /**
     * Tries to find the Registry container.
     *
     * @return Found (true) or not found (false)
     */
    public boolean findRegistry() {
        List<Container> registryContainers = docker.getDocker().listContainersCmd()
                .withLabelFilter(Utils.quickLabel(Constants.ContainerType.REGISTRY))
                .exec();

        if (registryContainers.size() > 1) {
            throw new FatalDockerMCError("Found multiple Registry containers.");

        } else if (registryContainers.size() == 1) {
            return true;

        } else {
            return false;
        }
    }

    /**
     * Creates the Registry container and confirms that it's up and running
     *
     * @return Successfully created (true) or failed (false)
     */
    public boolean createRegistry() {
        String containerID = this.createRegistryContainer();
        try {
            docker.getDocker().waitContainerCmd(containerID)
                    .exec(new WaitContainerResultCallback())
                    .awaitStarted(CONTAINER_START_DELAY, TimeUnit.SECONDS);

            TimeUnit.SECONDS.sleep(1);

            InspectContainerResponse r = docker.getDocker().inspectContainerCmd(containerID)
                    .exec();

            return r.getState().getRunning() == null ? false : r.getState().getRunning();

        } catch (InterruptedException e) {
            e.printStackTrace();
            return false;
        }
    }

    // ToDo: check if the certificates do exist

    /**
     * Creates a Registry container with no checks if it's actually running
     *
     * @return ContainerID
     */
    private String createRegistryContainer() {

        PortBinding portBinding = new PortBinding(Ports.Binding.bindPort(443), ExposedPort.tcp(5000));

        List<Bind> binds = Arrays.asList(
                new Bind("registry_data", new Volume("/var/lib/registry"), false),
                new Bind(Constants.REGISTRY_CERT_PATH,
                        new Volume("/auth")),
                new Bind(Constants.REGISTRY_CERT_PATH,
                        new Volume("/certs"))
        );

        List<String> envs = Arrays.asList("REGISTRY_AUTH=htpasswd", "REGISTRY_AUTH_HTPASSWD_REALM=Registry Realm",
                "REGISTRY_AUTH_HTPASSWD_PATH=/auth/" + Constants.REGISTRY_HTPASSWD_NAME,
                "REGISTRY_HTTP_TLS_CERTIFICATE=/certs/" + Constants.REGISTRY_CERT_NAME,
                "REGISTRY_HTTP_TLS_KEY=/certs/" + Constants.REGISTRY_KEY_NAME, "LOGLEVEL=INFO", "DEBUG=true");

        // ToDo: Labels
        CreateContainerResponse response = docker.getDocker().createContainerCmd(Constants.Images.REGISTRY.image())
                .withLabels(Utils.quickLabel(Constants.ContainerType.REGISTRY))
                .withPortBindings(portBinding)
                .withBinds(binds)
                .withEnv(envs)
                .exec();

        docker.getDocker().startContainerCmd(response.getId()).exec();

        System.out.println("Created registry " + response.getId() + ".");

        return response.getId();
    }


    /**
     * Tries to find the overnet network.
     *
     * @return Found (true) or not found (false)
     */
    public boolean findNetworkOvernet() {
        List<Network> networks = docker.getDocker().listNetworksCmd()
                .withNameFilter(Constants.NETW_OVERNET_NAME)
                .exec();

        if (networks.size() > 1) {
            throw new FatalDockerMCError("Found multiple overnet networks.");

        } else if (networks.size() == 1) {
            return true;

        } else {
            return false;
        }

    }

    /**
     * Creates the overnet network and confirms that it's up and running
     *
     * @return Successfully created (true) or failed (false)
     */
    public boolean createNetworkOvernet() {
        String networkID = this.createNetworkOvernetContainer();
        try {
            TimeUnit.SECONDS.sleep(NETWORK_CREATE_DELAY);

            try {
                docker.getDocker().inspectNetworkCmd()
                        .withNetworkId(networkID)
                        .exec();

            } catch (NotFoundException e1) {
                return false;
            }
            return true;

        } catch (InterruptedException e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Creates a overnet network with no checks if it's actually running
     *
     * @return ContainerID
     */
    private String createNetworkOvernetContainer() {
        Network.Ipam networkSettings = new Network.Ipam()
                .withConfig(new Network.Ipam.Config()
                        .withSubnet(Constants.SUBNET_OVERNET));

        CreateNetworkResponse response = docker.getDocker().createNetworkCmd()
                .withDriver("overlay")
                .withIpam(networkSettings)
                .withName(Constants.NETW_OVERNET_NAME)
                .withAttachable(true)
                .exec();

        return response.getId();
    }


    /**
     * Tries to find the api_net network.
     *
     * @return Found (true) or not found (false)
     */
    public boolean findNetworkApiNet() {
        List<Network> networks = docker.getDocker().listNetworksCmd()
                .withNameFilter(Constants.NETW_API_NAME)
                .exec();

        if (networks.size() > 1) {
            throw new FatalDockerMCError("Found multiple api networks.");

        } else if (networks.size() == 1) {
            return true;

        } else {
            return false;
        }
    }

    /**
     * Creates the api_net network and confirms that it's up and running
     *
     * @return Successfully created (true) or failed (false)
     */
    public boolean createNetworkApiNet() {
        String networkID = this.createNetworkApiNetContainer();
        try {
            TimeUnit.SECONDS.sleep(NETWORK_CREATE_DELAY);

            try {
                docker.getDocker().inspectNetworkCmd()
                        .withNetworkId(networkID)
                        .exec();

            } catch (NotFoundException e1) {
                return false;
            }
            return true;

        } catch (InterruptedException e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Creates an api_net network with no checks if it's actually running
     *
     * @return ContainerID
     */
    private String createNetworkApiNetContainer() {
        Network.Ipam networkSettings = new Network.Ipam()
                .withConfig(new Network.Ipam.Config()
                        .withSubnet(Constants.SUBNET_APINET));

        CreateNetworkResponse response = docker.getDocker().createNetworkCmd()
                .withDriver("overlay")
                .withIpam(networkSettings)
                .withName(Constants.NETW_API_NAME)
                .withAttachable(true)
                .exec();

        return response.getId();
    }


    /**
     * Tries to find the api_proxy container.
     *
     * @return Found (true) or not found (false)
     */
    public boolean findAPIProxy() {
        List<Container> registryContainers = docker.getDocker().listContainersCmd()
                .withLabelFilter(Utils.quickLabel(Constants.ContainerType.API_PROXY))
                .exec();

        if (registryContainers.size() > 1) {
            throw new FatalDockerMCError("Found multiple api_proxy containers.");

        } else if (registryContainers.size() == 1) {
            return true;

        } else {
            return false;
        }
    }

    /**
     * Creates the api_proxy container and confirms that it's up and running
     *
     * @return Successfully created (true) or failed (false)
     */
    public boolean createAPIProxy(boolean createCerts) {
        String containerID = this.createAPIProxyContainer(createCerts);

        try {
            docker.getDocker().waitContainerCmd(containerID)
                    .exec(new WaitContainerResultCallback())
                    .awaitStarted(CONTAINER_START_DELAY, TimeUnit.SECONDS);

            TimeUnit.SECONDS.sleep(1);

            InspectContainerResponse r = docker.getDocker().inspectContainerCmd(containerID)
                    .exec();

            return r.getState().getRunning() == null ? false : r.getState().getRunning();

        } catch (InterruptedException e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Creates a api_proxy container with no checks if it's actually running
     *
     * @return ContainerID
     */
    private String createAPIProxyContainer(boolean generateCerts) {

        List<Bind> binds = Arrays.asList(
                new Bind(Constants.API_CERTS_PATH, new Volume("/data/certs")),
                new Bind(Constants.DOCKER_SOCKET_PATH, new Volume("/var/run/docker.sock"))
        );

        List<String> envs = new ArrayList<>();
        if (generateCerts) {
            envs = Arrays.asList("CREATE_CERTS_WITH_PW=root", "CERT_HOSTNAME=" + Constants.APIPROXY_CONTAINER_NAME);
        }

        List<Container> apiProxyContainers = docker.getDocker().listContainersCmd()
                .withLabelFilter(Utils.quickLabel(Constants.ContainerType.API_PROXY))
                .withShowAll(true)
                .exec();

        if (apiProxyContainers.size() > 1) {
            System.out.println("[Info] Found multiple APIProxy containers.");
        }

        for (Container apiProxyContainer : apiProxyContainers) {
            docker.getDocker().removeContainerCmd(apiProxyContainer.getId()).exec();
            System.out.println("Removed existing API-proxy container.");
        }


        CreateContainerResponse response = docker.getDocker().createContainerCmd(Constants.Images.API_PROXY.image())
                .withLabels(Utils.quickLabel(Constants.ContainerType.API_PROXY))
                .withName(Constants.APIPROXY_CONTAINER_NAME)
                .withBinds(binds)
                .withEnv(envs)
                .exec();

        // ToDo: Change to NETW_API_NAME. When started from commandline, it can only attach one network
        docker.getDocker().connectToNetworkCmd()
                .withNetworkId(Constants.NETW_OVERNET_NAME)
                .withContainerId(response.getId())
                .exec();

        docker.getDocker().startContainerCmd(response.getId()).exec();

        System.out.println("Created API proxy " + response.getId() + ".");

        return response.getId();
    }



    public boolean findConfigWebserver() {
        List<Container> registryContainers = docker.getDocker().listContainersCmd()
                .withLabelFilter(Utils.quickLabel(Constants.ContainerType.CONFIG_WEBSERVER))
                .exec();

        if (registryContainers.size() > 1) {
            throw new FatalDockerMCError("Found multiple running config webserver containers.");

        } else if (registryContainers.size() == 1) {
            return true;

        } else {
            return false;
        }
    }

    public boolean createConfigWebserver() {
        String containerID = this.createConfigWebserverContainer();

        try {
            docker.getDocker().waitContainerCmd(containerID)
                    .exec(new WaitContainerResultCallback())
                    .awaitStarted(CONTAINER_START_DELAY, TimeUnit.SECONDS);

            TimeUnit.SECONDS.sleep(1);

            InspectContainerResponse r = docker.getDocker().inspectContainerCmd(containerID)
                    .exec();

            return r.getState().getRunning() == null ? false : r.getState().getRunning();

        } catch (InterruptedException e) {
            e.printStackTrace();
            return false;
        }
    }

    private String createConfigWebserverContainer() {

        List<Bind> binds = Arrays.asList(
                new Bind(Constants.CONFIG_PATH, new Volume("/webserver/data"), AccessMode.ro)
        );

        List<Container> webserverContainer = docker.getDocker().listContainersCmd()
                .withLabelFilter(Utils.quickLabel(Constants.ContainerType.CONFIG_WEBSERVER))
                .withShowAll(true)
                .exec();

        if (webserverContainer.size() > 1) {
            System.out.println("[Info] Found multiple APIProxy containers.");
        }

        for (Container apiProxyContainer : webserverContainer) {
            docker.getDocker().removeContainerCmd(apiProxyContainer.getId()).exec();
            System.out.println("Removed existing config webserver container.");
        }


        CreateContainerResponse response = docker.getDocker().createContainerCmd(Constants.Images.CONFIG_WEBSERVER.image())
                .withLabels(Utils.quickLabel(Constants.ContainerType.CONFIG_WEBSERVER))
                .withName(Constants.WEBSERVER_CONTAINER_NAME)
                .withBinds(binds)
                .exec();

        // ToDo: Change to NETW_API_NAME. When started from commandline, it can only attach one network
        docker.getDocker().connectToNetworkCmd()
                .withNetworkId(Constants.NETW_OVERNET_NAME)
                .withContainerId(response.getId())
                .exec();

        docker.getDocker().startContainerCmd(response.getId()).exec();

        System.out.println("Created webserver container " + response.getId() + ".");

        return response.getId();
    }

}
