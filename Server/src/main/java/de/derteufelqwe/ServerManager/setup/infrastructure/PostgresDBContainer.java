package de.derteufelqwe.ServerManager.setup.infrastructure;

import com.github.dockerjava.api.model.*;
import de.derteufelqwe.ServerManager.setup.templates.ExposableContainerTemplate;
import de.derteufelqwe.commons.Constants;
import de.derteufelqwe.commons.Utils;

import java.util.List;
import java.util.Map;

public class PostgresDBContainer extends ExposableContainerTemplate {


    public PostgresDBContainer() {
        super(Constants.POSTGRESDB_CONTAINER_NAME, Constants.Images.POSTGRES.image(), "512M", 0.5F, Constants.POSTGRESDB_PORT);
    }


    @Override
    protected Map<String, String> getContainerLabels() {
        Map<String, String> labels = super.getContainerLabels();

        labels.putAll(Utils.quickLabel(Constants.ContainerType.POSTGRES_DB));

        return labels;
    }

    @Override
    protected List<String> getEnvironmentVariables() {
        List<String> envs = super.getEnvironmentVariables();

        envs.add("POSTGRES_USER=admin");
        envs.add("POSTGRES_PASSWORD=password");
        envs.add("POSTGRES_DB=minecraft_logs");
        envs.add("POSTGRES_INITDB_ARGS=-E UTF8");   // UTF-8 character encoding required by django

        return envs;
    }

    @Override
    protected List<String> getNetworks() {
        List<String> networks = super.getNetworks();

        networks.add(Constants.NETW_OVERNET_NAME);

        return networks;
    }

    @Override
    protected List<Mount> getMounts() {
        List<Mount> mounts = super.getMounts();

        mounts.add(new Mount()
                .withSource("dmc_postgres_data")
                .withTarget("/var/lib/postgresql/data")
                .withType(MountType.VOLUME)
        );

        return mounts;
    }
}
