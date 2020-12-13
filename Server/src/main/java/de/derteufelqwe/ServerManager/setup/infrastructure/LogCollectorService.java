package de.derteufelqwe.ServerManager.setup.infrastructure;

import com.github.dockerjava.api.model.Mount;
import de.derteufelqwe.ServerManager.setup.templates.ServiceConstraints;
import de.derteufelqwe.ServerManager.setup.templates.ServiceTemplate;
import de.derteufelqwe.commons.Constants;
import de.derteufelqwe.commons.Utils;

import java.util.List;
import java.util.Map;

public class LogCollectorService extends ServiceTemplate {

    public LogCollectorService() {
        super("LogCollector", Constants.Images.LOGCOLLECTOR.image(), "512M", 0.5F, 1,
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

        labels.putAll(de.derteufelqwe.commons.Utils.quickLabel(Constants.ContainerType.LOGCOLLECTOR_POOL));

        return labels;
    }

    @Override
    protected Map<String, String> getContainerLabels() {
        Map<String, String> labels = super.getContainerLabels();

        labels.putAll(Utils.quickLabel(Constants.ContainerType.LOGCOLLECTOR));

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

}
