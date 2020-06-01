package de.derteufelqwe.commons.config.providers;

import com.google.gson.JsonElement;

import java.io.File;
import java.io.IOException;

public interface YamlConverter {

    String dumpJson(JsonElement element);
    void dumpJson(JsonElement element, File file) throws IOException;

    JsonElement loadJson(String yamlData);
    JsonElement loadJson(File file) throws IOException;

}
