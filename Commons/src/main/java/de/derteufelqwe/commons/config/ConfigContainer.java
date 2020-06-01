package de.derteufelqwe.commons.config;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class ConfigContainer {

    private Class clazz;
    private String filePath;
    private String fileName;
    private Object instance;

    public <T> T getInstance() {
        if (this.instance == null) {
            try {
                this.instance = clazz.newInstance();
            } catch (InstantiationException | IllegalAccessException e) {
                e.printStackTrace();
            }
        }

        return (T) this.instance;
    }

}
