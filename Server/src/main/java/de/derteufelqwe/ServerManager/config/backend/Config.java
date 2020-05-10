package de.derteufelqwe.ServerManager.config.backend;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import de.derteufelqwe.commons.Constants;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;

import java.io.*;
import java.util.*;

public class Config {

    private static Map<Class, ConfigContainer> configs = new HashMap<>();
    private static Gson gson = getGson();
    private static Yaml yaml = getYaml();



    private Config() {
    }


    private static Gson getGson() {
        GsonBuilder builder = new GsonBuilder()
                .setPrettyPrinting()
                .disableHtmlEscaping();

        return builder.create();
    }

    private static Yaml getYaml() {
        DumperOptions options = new DumperOptions();
        options.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
        options.setPrettyFlow(true);
        options.setIndent(2);

        MyRepresenter customRepresenter = new MyRepresenter();

        return new CommentsYAML(customRepresenter, options);
    }


    public static void registerConfig(Class clazz, String path, String fileName) {
        if (configs.containsKey(clazz))
            throw new RuntimeException(String.format("Class %s is already registered.", clazz.getSimpleName()));

        try {
            configs.put(clazz, new ConfigContainer(
                    clazz, fileName, path, clazz.newInstance()
            ));

        } catch (InstantiationException | IllegalAccessException e) {
            e.printStackTrace();
        }
    }


    public static <T> T get(Class<T> clazz) {
        return configs.get(clazz).getInstance();
    }

    public static <T> void setInstance(Class<T> clazz, T instance) {
        configs.get(clazz).setInstance(instance);
    }

    private static File getFile(Class clazz) {
        File directory = new File(Constants.CONFIG_PATH + configs.get(clazz).getFilePath());
        if (!directory.exists()) {
            directory.mkdirs();
        }

        File file = new File(Constants.CONFIG_PATH + configs.get(clazz).getFilePath() + configs.get(clazz).getFileName());

        return file;
    }

    public static void load(Class clazz) {
        File file = getFile(clazz);

        if (file.exists()) {
            try {
                Reader reader = new FileReader(file);
                configs.get(clazz).setInstance(yaml.loadAs(reader, clazz));
                reader.close();
            } catch (IOException e) {}

        } else {
            try {
                file.createNewFile();
                configs.get(clazz).setInstance(clazz.newInstance());
            } catch (InstantiationException | IllegalAccessException | IOException ex) {
                ex.printStackTrace();
            }
        }

    }

    public static void save(Class clazz) {
        File file = getFile(clazz);



        try {
            Writer writer = new FileWriter(file);
            writer.write(yaml.dump(configs.get(clazz).getInstance()));
            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    public static void saveAll() {
        for (Class c : configs.keySet()) {
            save(c);
        }
    }

    public static void loadAll() {
        for (Class c : configs.keySet()) {
            load(c);
        }
    }

}
