package de.derteufelqwe.ServerManager.config;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class ConfigContainer {

    private Class clazz;
    private String fileName;
    private String filePath;
    private Object instance;

}
