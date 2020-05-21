package de.derteufelqwe.commons.config.providers;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.yaml.snakeyaml.Yaml;

/**
 * Used to get the Yaml instance for the config
 */
public interface YamlProvider {

    ObjectMapper getMapper();

}
