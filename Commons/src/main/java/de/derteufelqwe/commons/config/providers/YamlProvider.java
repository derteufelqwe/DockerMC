package de.derteufelqwe.commons.config.providers;

import shaded.mcp.com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Used to get the Yaml instance for the config
 */
public interface YamlProvider {

    ObjectMapper getMapper();

}
