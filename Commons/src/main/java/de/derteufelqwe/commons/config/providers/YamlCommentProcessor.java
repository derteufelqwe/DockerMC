package de.derteufelqwe.commons.config.providers;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import de.derteufelqwe.commons.config.annotations.Comment;
import de.derteufelqwe.commons.config.exceptions.YAMLWalkException;
import de.derteufelqwe.commons.containers.Pair;
import lombok.Data;
import lombok.SneakyThrows;
import org.apache.commons.lang3.StringUtils;

import java.lang.reflect.Field;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * This class parses and adds comments to the YAML output
 * Adding comments to Map values is NOT supported
 */
public class YamlCommentProcessor {

    private Pattern RE_INDENT = Pattern.compile("^(\\s+)?(.+)");

    private Gson gson;
    private String yamlOutput;
    private Class<?> clazz;
    private JsonObject halfSerializedObject;
    private int indent;

    private YamlComments comments = new YamlComments();
    private List<String> lines = new ArrayList<>();

    private String output;


    public YamlCommentProcessor(Gson gson, String yamlOutput, Class<?> clazz, JsonObject halfSerializedObject, int indent) {
        this.gson = gson;
        this.yamlOutput = yamlOutput;
        this.clazz = clazz;
        this.halfSerializedObject = halfSerializedObject;
        this.indent = indent;

        this.lines = Arrays.asList(yamlOutput.split("\n"));
    }

    /**
     * This method can't be called twice
     * @return
     */
    public String process() {
        if (this.output != null) {
            return this.output;
        }

        // Generate comments
        this.parseCommentsFromClass(this.clazz, this.halfSerializedObject.entrySet(), this.comments);

        // Add the comments
        List<String> newLines = new ArrayList<>(this.lines);
        this.addComment(newLines, this.comments, 0, new ArrayList<>());

        this.output = String.join("\n", newLines);

        return this.output;
    }


    /**
     * Analyzes the class that got serialized and parses the @{@link Comment} annotations, to create a map containing
     * the comments for each variable
     * @param clazz
     * @param jsonData
     * @param resultMap
     */
    protected void parseCommentsFromClass(Class<?> clazz, Set<Map.Entry<String, JsonElement>> jsonData, YamlComments resultMap) {

        for (Map.Entry<String, JsonElement> entry : jsonData) {
            try {
                Field field = clazz.getDeclaredField(entry.getKey());

                Comment comment = field.getAnnotation(Comment.class);
                if (comment == null) {
                    continue;
                }
                resultMap.getChildren().put(field.getName(), new YamlComments(comment.value()));

                if (field.getType() == Map.class) {
                    continue;

                } else if (entry.getValue() instanceof JsonObject) {
                    this.parseCommentsFromClass(field.getType(), entry.getValue().getAsJsonObject().entrySet(), resultMap.getChildren().get(field.getName()));
                }

            } catch (NoSuchFieldException ignored) {}
        }

        return;
    }

    protected int findInYaml(List<String> yamlLines, List<String> toSearch) throws YAMLWalkException {
        return this.findInYaml(yamlLines, toSearch, 0);
    }

    /**
     * Walks a path down a YAML file and returns the result line
     * If the file looks like this:
     * name: Name
     * su2:
     *     a: a
     *     b: b
     *
     * And the path looks like the [name], 0 is returned
     * If it looks like this [sub, b], 2 is returned
     *
     * @param yamlLines
     * @param toSearch
     * @param offset
     * @return
     */
    protected int findInYaml(List<String> yamlLines, List<String> toSearch, int offset) throws YAMLWalkException {
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

    /**
     * Adds the comments found by parseCommentsFromClass(...) to a new list of lines.
     * @param newLines Copy of this.lines
     * @param comments CommentsMap object
     * @param increment Number of comments added so far
     * @param toSearch Search history to search through a yaml file
     */
    protected int addComment(List<String> newLines, YamlComments comments, int increment, List<String> toSearch) {
        for (String key : comments.getChildren().keySet()) {
            YamlComments value = comments.getChildren().get(key);
            String comment = value.getComment();
            Map<String, YamlComments> children = value.getChildren();

            List<String> newToSearch = new ArrayList<>(toSearch);
            newToSearch.add(key);

            if (comment != null) {
                int index = this.findInYaml(this.lines, newToSearch);

                newLines.add(index + increment, StringUtils.repeat(" ", this.indent * toSearch.size()) + "# " + comment);
                increment++;
            }

            if (children.size() != 0) {
                increment = addComment(newLines, value, increment, newToSearch);
            }

        }

        return increment;
    }


    /**
     * Helper object, that represents the found comments
     */
    @Data
    protected class YamlComments {

        private String comment;
        private Map<String, YamlComments> children = new LinkedHashMap<>();     // Must e a LinkedHashMap to maintain the order

        public YamlComments() {

        }

        public YamlComments(String comment) {
            this.comment = comment;
        }

        @Override
        public String toString() {
            return String.format("YamlComments<%s, %s>", comment, children.size() > 0);
        }

    }

}

