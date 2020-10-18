package de.derteufelqwe.commons;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import de.derteufelqwe.commons.config.Config;
import de.derteufelqwe.commons.config.annotations.Comment;
import de.derteufelqwe.commons.config.providers.DefaultGsonProvider;
import de.derteufelqwe.commons.config.providers.DefaultYamlConverter;
import de.derteufelqwe.commons.containers.Pair;

import java.lang.reflect.Field;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


public class Testing {

    private Map<String, Pair<String, Object>> commentMap = new HashMap<>();
    private List<String> lines = new ArrayList<>();
    Pattern RE_LINE = Pattern.compile("^(\\s+)?([^:]+):(.+)?");
    Pattern RE_LIST = Pattern.compile("^\\s+-");
    private int increment = 0;

    public Testing() {
    }


    public void run() {
        DefaultYamlConverter converter = new DefaultYamlConverter();
        Gson gson = new DefaultGsonProvider().getGson();

        JsonObject jsonObject = (JsonObject) gson.toJsonTree(new Data());

        this.index(Data.class, jsonObject.entrySet(), this.commentMap);

        String serialized = converter.dumpJson(jsonObject);
        this.lines = new ArrayList<>(Arrays.asList(serialized.split("\n")));

        this.parse(jsonObject, this.commentMap, 0);

        return;
    }


    public void index(Class<?> clazz, Set<Map.Entry<String, JsonElement>> data, Map<String, Pair<String, Object>> map) {

        for (Map.Entry<String, JsonElement> entry : data) {
            try {
                Field field = clazz.getDeclaredField(entry.getKey());
                if (field.getClass().isPrimitive()) {
                    continue;
                } else if (field.getType() == Map.class) {
                    continue;
                }

                Pair<String, Object> pair = new Pair<>();
                Comment comment = field.getAnnotation(Comment.class);
                map.put(field.getName(), new Pair<>(comment != null ? comment.value() : null, new HashMap<String, Pair<String, Object>>()));

                if (entry.getValue() instanceof JsonObject) {
                    this.index(field.getType(), entry.getValue().getAsJsonObject().entrySet(), (Map<String, Pair<String, Object>>) map.get(field.getName()).getB());
                }

            } catch (NoSuchFieldException ignored) {}
        }

        return;
    }


    public int parse(JsonObject data, Map<String, Pair<String, Object>> comments, int index) {

        for (Map.Entry<String, JsonElement> entry : data.entrySet()) {

            if (comments.containsKey(entry.getKey())) {
                String comment = comments.get(entry.getKey()).getA();
                if (comment != null) {
                    System.out.println(index + " -> " + entry.getKey() + " -> " + comment);
                }
            }

            index++;

            if (entry.getValue() instanceof JsonObject) {
                if (comments.containsKey(entry.getKey())) {
                    index = parse(entry.getValue().getAsJsonObject(), (Map<String, Pair<String, Object>>) comments.get(entry.getKey()).getB(), index);
                }

            } else if (entry.getValue() instanceof JsonPrimitive) {

            } else {
                String serialized = new DefaultYamlConverter().dumpJson(entry.getValue());
                index += serialized.split("\n").length;
            }

        }

        return index;
    }


    public static void main(String[] args) {
        new Testing().run();
    }



}
