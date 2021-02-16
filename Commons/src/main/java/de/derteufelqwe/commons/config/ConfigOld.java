package de.derteufelqwe.commons.config;

import com.google.gson.Gson;
import de.derteufelqwe.commons.config.providers.GsonProvider;
import de.derteufelqwe.commons.config.providers.YamlConverter;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class ConfigOld {

    private Map<Class, ConfigContainer> configs = new HashMap<>();
    private YamlConverter converter;
    private Gson gson;
    private String basePath;

    public ConfigOld(YamlConverter converter, GsonProvider gsonProvider, String basePath) {
        this.converter = converter;
        this.gson = gsonProvider.getGson();
        this.basePath = basePath;
    }

    public ConfigOld(YamlConverter converter, GsonProvider gsonProvider) {
        this(converter, gsonProvider, ".");
    }


    public void registerConfig(Class clazz, String path, String fileName) {
        if (configs.containsKey(clazz))
            throw new RuntimeException(String.format("Class %s is already registered.", clazz.getSimpleName()));

        try {
            configs.put(clazz, new ConfigContainer(
                    clazz, path, fileName, clazz.newInstance()
            ));

        } catch (InstantiationException | IllegalAccessException e) {
            e.printStackTrace();
            System.err.println("Failed to instanciate class " + clazz.getName() + ".");
        }
    }

    public void registerConfig(Class clazz, String filename) {
        this.registerConfig(clazz, ".", filename);
    }


    public <T> T get(Class<T> clazz) {
        return configs.get(clazz).getInstance();
    }

    public <T> void setInstance(Class<T> clazz, T instance) {
        configs.get(clazz).setInstance(instance);
    }

    private File getFile(Class clazz) {
        String filePath = configs.get(clazz).getFilePath();
        String fileName = configs.get(clazz).getFileName();
        File directory = new File(filePath);
        if (!directory.exists()) {
            directory.mkdirs();
        }

        File file = new File(this.basePath + "/" + filePath + "/" + fileName);

        return file;
    }


    public void load(Class clazz) {
        File file = getFile(clazz);

        if (file.exists()) {
            try {
                configs.get(clazz).setInstance(gson.fromJson(converter.loadJson(file), clazz));

            } catch (IOException e) {
                e.printStackTrace();
            }

        } else {
            try {
                file.createNewFile();
                configs.get(clazz).setInstance(clazz.newInstance());

            } catch (InstantiationException | IllegalAccessException | IOException e) {
                e.printStackTrace();
                System.err.println("Failed to instanciate class " + clazz.getName() + ".");
            }
        }

    }

    public void save(Class clazz) {
        File file = getFile(clazz);
        Object instance = configs.get(clazz).getInstance();

        try {
            converter.dumpJson(instance, file);

        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    public void saveAll() {
        for (Class c : configs.keySet()) {
            save(c);
        }
    }

    public void loadAll() {
        for (Class c : configs.keySet()) {
            load(c);
        }
    }


}
