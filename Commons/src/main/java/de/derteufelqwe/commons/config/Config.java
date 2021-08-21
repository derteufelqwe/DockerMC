package de.derteufelqwe.commons.config;

import com.google.gson.Gson;
import de.derteufelqwe.commons.config.providers.GsonProvider;
import de.derteufelqwe.commons.config.providers.YamlConverter;
import de.derteufelqwe.commons.misc.VFile;
import org.apache.commons.io.IOUtils;

import java.io.*;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

public class Config<A> {

    private YamlConverter converter;
    private Gson gson;
    private VFile file;

    private A instance;
    

    public Config(YamlConverter converter, GsonProvider gsonProvider, String fileName, A instance) {
        this.converter = converter;
        this.gson = gsonProvider.getGson();
        this.instance = instance;
        this.file = new VFile(fileName);

        if (file.getParentFile() != null)
            file.getParentFile().mkdirs();
    }

    public Config(VFile file, YamlConverter converter, GsonProvider gsonProvider, A instance) {
        this.file = file;
        this.converter = converter;
        this.gson = gsonProvider.getGson();
        this.instance = instance;

        if (file.getParentFile() != null)
            file.getParentFile().mkdirs();
    }


    public A get() {
        return this.instance;
    }

    public void set(A instance) {
        this.instance = instance;
    }


    public void load() {
        try {
            if (!file.createNewFile()) {
                InputStream is = file.getInputStream();
                String data = IOUtils.toString(is, Charset.defaultCharset());
                is.close();

                this.instance = gson.fromJson(converter.loadJson(data), (Class<A>) this.instance.getClass());

            } else {
                this.save();
            }

        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    public void save() {
        try {
            String data = converter.dumpJson(instance);
            OutputStream os = file.getOutputStream();
            os.write(data.getBytes(StandardCharsets.UTF_8));
            os.close();

        } catch (IOException e) {
            e.printStackTrace();
        }

    }

}
