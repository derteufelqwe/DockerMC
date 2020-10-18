package de.derteufelqwe.commons.config.providers;

import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;

/**
 * Implementation of {@link YamlProvider}, which creates a basic {@link Yaml} instance.
 */
public class DefaultYamlProvider implements YamlProvider {

    public DefaultYamlProvider() {
    }

    public DumperOptions getOptions() {
        DumperOptions options = new DumperOptions();
        options.setPrettyFlow(true);
        options.setIndent(2);
        options.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);

        return options;
    }

    @Override
    public Yaml getYaml() {
        return new Yaml(getOptions());
    }

}
