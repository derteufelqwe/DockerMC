package de.derteufelqwe.ServerManager.config;

import org.yaml.snakeyaml.introspector.Property;
import org.yaml.snakeyaml.nodes.MappingNode;
import org.yaml.snakeyaml.nodes.Node;
import org.yaml.snakeyaml.nodes.NodeTuple;
import org.yaml.snakeyaml.nodes.Tag;
import org.yaml.snakeyaml.representer.Represent;
import org.yaml.snakeyaml.representer.Representer;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class MyRepresenter extends Representer {

    public MyRepresenter() {

    }

    /**
     * Custom version of representJavaBean, which removes all entries, which are annotated with @Ignore.
     */
    @Override
    protected MappingNode representJavaBean(Set<Property> properties, Object javaBean) {
        Class clazz = javaBean.getClass();
        Set<Property> newPropertys = new HashSet<>();
        Map<String, Field> recursiveFields = this.recursiveGetDeclaredFields(clazz);

        for (Property property : properties) {
            String propName = property.getName();

            if (recursiveFields.containsKey(propName)) {
                Field field = recursiveFields.get(propName);
                if (!field.isAnnotationPresent(Ignore.class)) {
                    newPropertys.add(property);
                }
            }
        }

        return super.representJavaBean(newPropertys, javaBean);
    }

    @Override
    protected NodeTuple representJavaBeanProperty(Object javaBean, Property property, Object propertyValue, Tag customTag) {
        return super.representJavaBeanProperty(javaBean, property, propertyValue, customTag);
    }


    private Map<String, Field> recursiveGetDeclaredFields(Class clazz) {
        Map<String, Field> data = new HashMap<>();

        this.recursiveGetDeclaredFields(clazz, data);

        return data;
    }

    private Map<String, Field> recursiveGetDeclaredFields(Class clazz, Map<String, Field> data) {
        for (Field field : clazz.getDeclaredFields()) {
            data.put(field.getName(), field);
        }

        if (clazz.getSuperclass() != null) {
            this.recursiveGetDeclaredFields(clazz.getSuperclass(), data);
        }

        return data;
    }

}
