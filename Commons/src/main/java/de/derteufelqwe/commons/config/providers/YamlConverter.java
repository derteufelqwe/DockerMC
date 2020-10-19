package de.derteufelqwe.commons.config.providers;

import com.google.gson.JsonElement;

import java.io.File;
import java.io.IOException;

/**
 * Interface which provides methods to save and load serialized data
 */
public interface YamlConverter {

    // Serializing methods
    String dumpJson(Object toSerialize);
    void dumpJson(Object toSerialize, File file) throws IOException;

    // Deserializing methods
    JsonElement loadJson(String yamlData);
    JsonElement loadJson(File file) throws IOException;

}
