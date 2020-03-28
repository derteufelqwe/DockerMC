package de.derteufelqwe.ServerManager.config;

import org.apache.commons.compress.utils.Lists;
import org.apache.commons.lang.StringUtils;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.BaseConstructor;
import org.yaml.snakeyaml.representer.Representer;
import org.yaml.snakeyaml.resolver.Resolver;

import java.io.Writer;
import java.lang.reflect.Field;
import java.util.*;
import java.util.regex.Pattern;

/**
 * My version of the YAML class, which can also add comments.
 */
public class CommentsYAML extends Yaml {

    private final Pattern LEADING_WHITESPACE_REGEX = Pattern.compile("^\\s+");
    private final int MAX_ITERATIONS = 5;
    private int indent;


    public CommentsYAML(Representer representer, DumperOptions dumperOptions) {
        super(representer, dumperOptions);
        this.indent = dumperOptions.getIndent();
    }


    protected void parseConfig(Map<String, String> varCommentMap, Class clazz, int iteration) {
        for (Field field : clazz.getDeclaredFields()) {
            if (field.isAnnotationPresent(YAMLComment.class)) {
                varCommentMap.put(StringUtils.repeat(" ", this.indent * iteration) + field.getName(),
                        field.getAnnotation(YAMLComment.class).value());
            }

            // Only check if the class is not from java itself
            if (iteration < MAX_ITERATIONS && !field.getType().getName().startsWith("java.lang")) {
                parseConfig(varCommentMap, field.getType(), iteration + 1);
            }
        }
    }


    protected String addComments(String serializedData, Class clazz) {
        Map<String, String> varCommentMap = new HashMap<>();
        parseConfig(varCommentMap, clazz, 0);

        List<String> lines = new ArrayList<>(Arrays.asList(serializedData.split("\n")));
        List<String> newLines = new ArrayList<>();

        for (String line : lines) {
            String[] splitLine = line.split(":");

            if (varCommentMap.containsKey(splitLine[0])) {
                String splitLineParsed = LEADING_WHITESPACE_REGEX.matcher(splitLine[0]).replaceAll("");
                int indent = splitLine[0].length() - splitLineParsed.length();

                newLines.add(StringUtils.repeat(" ", indent) + "# " + varCommentMap.get(splitLine[0]));
            }

            newLines.add(line);
        }

        return String.join("\n", newLines);
    }

    @Override
    public String dumpAll(Iterator<?> data) {
        List<Object> objectList = Lists.newArrayList(data);
        String serializedData = super.dumpAll(objectList.iterator());
        Class clazz = objectList.get(0).getClass();

        return this.addComments(serializedData, clazz);
    }

}
