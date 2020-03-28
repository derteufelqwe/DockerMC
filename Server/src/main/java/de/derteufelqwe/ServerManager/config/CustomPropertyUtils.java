package de.derteufelqwe.ServerManager.config;

import org.yaml.snakeyaml.introspector.BeanAccess;
import org.yaml.snakeyaml.introspector.Property;
import org.yaml.snakeyaml.introspector.PropertyUtils;

import java.util.Map;

/**
 * Will later be used to keep the config in order
 */
public class CustomPropertyUtils extends PropertyUtils {


    public CustomPropertyUtils() {
        super();
    }

    protected Map<String, Property> getPropertiesMap(Class<?> type, BeanAccess bAccess) {
        Map<String, Property> properties = super.getPropertiesMap(type, bAccess);

        return properties;
    }


}