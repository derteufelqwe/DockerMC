package de.derteufelqwe.commons.config;

import com.google.gson.Gson;
import de.derteufelqwe.commons.config.providers.GsonProvider;
import de.derteufelqwe.commons.config.providers.YamlConverter;

import java.io.File;
import java.io.IOException;

public class Config<A> {

    private YamlConverter converter;
    private Gson gson;
    private String fileName;

    private A instance;


    public Config(YamlConverter converter, GsonProvider gsonProvider, String fileName, A instance) {
        this.converter = converter;
        this.gson = gsonProvider.getGson();
        this.fileName = fileName;
        this.instance = instance;
    }


    public A get() {
        return this.instance;
    }

    public void set(A instance) {
        this.instance = instance;
    }

    private File getFile() {
        File file = new File(fileName);
        if (file.getParentFile() != null)
            file.getParentFile().mkdirs();

        return file;
    }

    public void load() {
        File file = getFile();

        try {
            if (!file.createNewFile()) {
                this.instance = gson.fromJson(converter.loadJson(file), (Class<A>) this.instance.getClass());

            } else {
                this.save();
            }

        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    public void save() {
        File file = getFile();

        try {
            converter.dumpJson(instance, file);

        } catch (IOException e) {
            e.printStackTrace();
        }

    }

}
