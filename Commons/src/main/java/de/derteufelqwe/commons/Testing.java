package de.derteufelqwe.commons;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import de.derteufelqwe.commons.config.Config;
import de.derteufelqwe.commons.config.annotations.Comment;
import de.derteufelqwe.commons.config.exceptions.YAMLWalkException;
import de.derteufelqwe.commons.config.providers.DefaultGsonProvider;
import de.derteufelqwe.commons.config.providers.DefaultYamlConverter;
import de.derteufelqwe.commons.config.providers.YamlCommentProcessor;
import de.derteufelqwe.commons.containers.Pair;
import lombok.SneakyThrows;

import java.lang.reflect.Field;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;


public class Testing {

    private Map<String, Pair<String, Object>> commentMap = new LinkedHashMap<>();
    private List<String> lines = new ArrayList<>();
    Pattern RE_LINE = Pattern.compile("^(\\s+)?([^:]+):(.+)?");
    Pattern RE_LIST = Pattern.compile("^\\s+-");
    Pattern RE_INDENT = Pattern.compile("^(\\s+)?(.+)");
    Pattern RE_INDENT_LENTH = Pattern.compile("^(\\s+)?");
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
        List<String> newLines = new ArrayList<>(this.lines);

//        int x = this.findInYaml(this.lines, Arrays.asList("ag2"));

        addComment(lines, newLines, commentMap, 0, new ArrayList<>());

        String res = String.join("\n", newLines);

        return;
    }


    public void run2() {
        DefaultYamlConverter converter = new DefaultYamlConverter();
        Gson gson = new DefaultGsonProvider().getGson();

        JsonObject jsonObject = (JsonObject) gson.toJsonTree(new Data());
        String serialized = converter.dumpJson(jsonObject);

        YamlCommentProcessor processor = new YamlCommentProcessor(gson, serialized, Data.class, jsonObject, converter.getYamlIndent());
        String output = processor.process();
        System.out.println(output);


        JsonObject ret = (JsonObject) converter.loadJson(output);

        return;
    }

    @SneakyThrows
    public void run3() {
        Field field = Data.class.getDeclaredField("map");

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

    public void addComment(List<String> oldLines, List<String> newLines, Map<String, Pair<String, Object>> comments, int increment, List<String> toSearch) {
        for (String key : comments.keySet()) {
            Pair<String, Object> value = comments.get(key);
            String comment = value.getA();
            Map<String, Pair<String, Object>> children = (Map<String, Pair<String, Object>>) value.getB();

            List<String> newToSearch = new ArrayList<>(toSearch);
            newToSearch.add(key);

            if (comment != null) {
                int index = this.findInYaml(oldLines, newToSearch);

                newLines.add(index + increment, "# " + comment);
                increment++;
            }

            if (children.size() != 0) {
                addComment(oldLines, newLines, children, increment, newToSearch);
            }

        }
    }

    public int findInYaml(List<String> yamlLines, List<String> toSearch) {
        return this.findInYaml(yamlLines, toSearch, 0);
    }

    public int findInYaml(List<String> yamlLines, List<String> toSearch, int offset) {
        // Start and end positions of the sliced YAML block
        int start = -1;
        int end = -1;

        for (int i = 0; i < yamlLines.size(); i++) {
            String line = yamlLines.get(i);

            // Find the opening tag
            if (start == -1 && line.startsWith(toSearch.get(0))) {
                start = i;
                continue;
            }

            // If the opening tag was found, search for a the closing tag
            if (start != -1) {
                Matcher m = RE_INDENT.matcher(line);
                m.matches();

                // Check if the indent is 0 again or if the file has ended
                if (m.group(1) == null || m.group(1).length() == 0 || i == yamlLines.size() - 1) {
                    end = i - 1;
                    if (i == yamlLines.size() - 1) {
                        end = i;
                    }

                    // Return if no more statements to check for are left
                    if (start == end || toSearch.size() == 1) {
                        return start + offset;
                    }

                    // Create the new sliced out YAML part
                    List<String> oldSlice = yamlLines.subList(start + 1, end + 1);
                    List<String> slice = new ArrayList<>();

                    for (String slc : oldSlice) {
                        Matcher m2 = RE_INDENT.matcher(slc);
                        m2.matches();
                        slice.add(m2.group(2));
                    }

                    return this.findInYaml(slice, toSearch.subList(1, toSearch.size()), start + 1);
                }
            }

        }

        if (start + offset == -1) {
            throw new YAMLWalkException("Failed to walk the path '%s' in the YAML file.", String.join(" -> ", toSearch));
        }

        return start + offset;
    }


    public static void main(String[] args) {
        new Testing().run2();
    }



}
