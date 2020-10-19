package de.derteufelqwe.commons.config.providers;

import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;

/**
 * Interface to provide a method to create a {@link Yaml} instance,
 * which is used to serialize {@link java.util.Map}s created by the {@link GsonProvider}
 */
public interface YamlProvider {

    DumperOptions getOptions();
    Yaml getYaml();

}
