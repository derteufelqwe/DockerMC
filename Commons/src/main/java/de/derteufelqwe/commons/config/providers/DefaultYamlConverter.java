package de.derteufelqwe.commons.config.providers;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import com.google.gson.internal.$Gson$Preconditions;
import com.google.gson.reflect.TypeToken;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.NotImplementedException;
import org.checkerframework.checker.units.qual.C;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.error.YAMLException;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.Map;

public class DefaultYamlConverter implements YamlConverter {

    protected Yaml yaml;
    protected Gson gson;
    protected TypeToken<Map<String, Object>> mapTypeToken = new TypeToken<Map<String, Object>>() {};
    protected TypeToken<String> stringTypeToken = new TypeToken<String>() {};


    public DefaultYamlConverter() {
        this(new DefaultYamlProvider(), new DefaultGsonProvider());
    }

    public DefaultYamlConverter(YamlProvider yamlProvider, GsonProvider gsonProvider) {
        this.yaml = yamlProvider.getYaml();
        this.gson = gsonProvider.getGson();
    }

    /**
     * @deprecated Use {@link #DefaultYamlConverter(YamlProvider, GsonProvider)} instead
     */
    @Deprecated
    public DefaultYamlConverter(Yaml yaml, Gson gson) {
        this.yaml = yaml;
        this.gson = gson;
    }


    @Override
    public String dumpJson(JsonElement element) {
        if (element instanceof JsonPrimitive) {
            return yaml.dump(gson.fromJson(element, stringTypeToken.getType()));

        } else if (element instanceof JsonObject) {
            return yaml.dump(gson.fromJson(element, mapTypeToken.getType()));

        } else {
            throw new NotImplementedException("Objects of type " + element.getClass() + " are not supported (yet).");
        }
    }

    @Override
    public void dumpJson(JsonElement element, File file) throws IOException {
        String data = this.dumpJson(element);

        FileUtils.writeStringToFile(file, data, "UTF-8");
    }

    @Override
    public JsonElement loadJson(String yamlData) {
        try {
            // For complex data types
            return gson.toJsonTree(yaml.loadAs(yamlData, Map.class));

        } catch (YAMLException e) {
            // For primary data types like Strings
            return gson.toJsonTree(yaml.load(yamlData));
        }
    }

    @Override
    public JsonElement loadJson(File file) throws IOException {
        String data = FileUtils.readFileToString(file, "UTF-8");

        return this.loadJson(data);
    }

}
