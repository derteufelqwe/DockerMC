package de.derteufelqwe.commons.config.providers;

import com.fasterxml.jackson.databind.Module;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;

import java.util.ArrayList;
import java.util.List;

public class DefaultYamlProvider implements YamlProvider {

    @Override
    public ObjectMapper getMapper() {
        ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
        mapper.findAndRegisterModules();

        for (Module m : this.getModules()) {
            mapper.registerModule(m);
        }

        return mapper;
    }

    protected List<Module> getModules() {
        List<Module> modules = new ArrayList<>();

        SimpleModule module = new SimpleModule("DefaultModule");

        modules.add(module);

        return modules;
    }

}
