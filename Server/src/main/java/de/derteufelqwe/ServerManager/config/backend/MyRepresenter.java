package de.derteufelqwe.ServerManager.config.backend;

import org.yaml.snakeyaml.introspector.Property;
import org.yaml.snakeyaml.nodes.MappingNode;
import org.yaml.snakeyaml.representer.Representer;

import java.lang.reflect.Field;
import java.util.*;
import java.util.stream.Collectors;

public class MyRepresenter extends Representer {

    public MyRepresenter() {

    }

    /**
     * Custom version of representJavaBean, which removes all entries, which are annotated with @Ignore.
     */
    @Override
    protected MappingNode representJavaBean(Set<Property> properties, Object javaBean) {
        Class clazz = javaBean.getClass();
        Set<Property> newPropertys = new LinkedHashSet<>();
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

    /**
     * Modified version of getProperties, which sorts the properties by the order in which they appear in the class
     */
    @Override
    protected Set<Property> getProperties(Class<?> type) {
        Set<Property> properties = super.getProperties(type);
        List<String> fieldNameList = this.getFieldNamesRecursively(type);
        List<Property> sortedProperties = this.getFilledPropertyList(properties.size());

        for (Property property : properties) {
            String propName = property.getName();
            int index = fieldNameList.indexOf(propName);

            sortedProperties.add(index, property);
            sortedProperties.remove(index + 1);
        }

        Set<Property> sortedSet = new LinkedHashSet<>();
        for (Property property : sortedProperties) {
            sortedSet.add(property);
        }

        return sortedSet;
    }

    /**
     * Recursively maps all field name to the field object
     *
     * @param clazz Class to map
     * @return
     */
    private Map<String, Field> recursiveGetDeclaredFields(Class clazz) {

        return this.recursiveGetDeclaredFields(clazz, new HashMap<>());
    }

    /**
     * Recursively maps all field name to the field object
     *
     * @param clazz Class to map
     * @param data  Data from subclass
     * @return
     */
    private Map<String, Field> recursiveGetDeclaredFields(Class clazz, Map<String, Field> data) {
        for (Field field : clazz.getDeclaredFields()) {
            data.put(field.getName(), field);
        }

        if (clazz.getSuperclass() != null) {
            return this.recursiveGetDeclaredFields(clazz.getSuperclass(), data);
        }

        return data;
    }


    /**
     * Recursively create a list of field names of a class.
     *
     * @param clazz Class to analyze
     * @return An ordered list
     */
    private List<String> getFieldNamesRecursively(Class clazz) {

        return this.getFieldNamesRecursively(clazz, new LinkedList<>());
    }

    /**
     * Recursively create a list of field names of a class.
     *
     * @param clazz Class to analyze
     * @param data  Preexisting data
     * @return An ordered list
     */
    private List<String> getFieldNamesRecursively(Class clazz, List<String> data) {
        List<Field> declaredFields = Arrays.stream(clazz.getDeclaredFields())
                .filter(f -> !f.isAnnotationPresent(Ignore.class))
                .collect(Collectors.toList());

        for (int i = 0; i < declaredFields.size(); i++) {
            Field field = declaredFields.get(i);

            data.add(i, field.getName());
        }

        if (clazz.getSuperclass() != null) {
            return this.getFieldNamesRecursively(clazz.getSuperclass(), data);
        }

        return data;
    }

    /**
     * Creates a with null prefilled list.
     *
     * @param size Size of the list
     */
    private List<Property> getFilledPropertyList(int size) {
        Property[] properties = new Property[size];
        Arrays.fill(properties, null);

        return new LinkedList<>(Arrays.asList(properties));
    }

}
