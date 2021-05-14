package de.derteufelqwe.ServerManager.setup.infrastructure;

import com.github.dockerjava.api.model.Driver;
import com.github.dockerjava.api.model.Mount;
import de.derteufelqwe.ServerManager.setup.templates.ServiceConstraints;
import de.derteufelqwe.ServerManager.setup.templates.ServiceTemplate;
import de.derteufelqwe.commons.Constants;
import de.derteufelqwe.commons.Utils;

import java.util.List;
import java.util.Map;

public class NodeWatcherService extends ServiceTemplate {

    /**
     * Note: Since the useGlobalMode is true the replications count is irrelevant.
     */
    public NodeWatcherService() {
        super("NodeWatcher", Constants.Images.NODEWATCHER.image(), "512M", 0.5F, 1,
                new ServiceConstraints(1));
    }

    @Override
    protected boolean useGlobalMode() {
        return true;
    }

    /**
     * ToDo: Override to use image from official docker registry
     */
    @Override
    protected String getImageName() {
        return this.image;
    }

    @Override
    protected Map<String, String> getServiceLabels() {
        Map<String, String> labels = super.getServiceLabels();

        labels.putAll(Utils.quickLabel(Constants.ContainerType.NODE_WATCHER_POOL));

        return labels;
    }

    @Override
    protected Map<String, String> getContainerLabels() {
        Map<String, String> labels = super.getContainerLabels();

        labels.putAll(Utils.quickLabel(Constants.ContainerType.NODE_WATCHER));

        return labels;
    }

    @Override
    protected List<Mount> getMountVolumes() {
        List<Mount> mounts = super.getMountVolumes();

        mounts.add(new Mount()
                .withSource("/var/run/docker.sock")
                .withTarget("/var/run/docker.sock")
        );

        return mounts;
    }

    @Override
    protected List<String> getEnvs() {
        List<String> envs = super.getEnvs();

        envs.add("HOSTNAME={{ .Node.Hostname }}");

        return envs;
    }

    @Override
    protected Driver getLogDriver() {
        return new Driver()
                .withName(Constants.DOCKER_DRIVER_PLUGIN_NAME);
//                .withName("dmcdriver");
    }
}
