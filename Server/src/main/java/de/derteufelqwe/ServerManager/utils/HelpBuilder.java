package de.derteufelqwe.ServerManager.utils;

import com.sun.istack.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.apache.commons.lang.StringUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

public class HelpBuilder {

    private String title;
    private String leftPadding = "  ";
    private List<Entry> entries = new ArrayList<>();


    public HelpBuilder(String title, int leftPadding) {
        this.title = title;
        this.leftPadding = StringUtils.repeat(" ", leftPadding);
    }

    public HelpBuilder(String title) {
        this(title, 2);
    }


    public HelpBuilder addEntry(String command, String help) {
        this.entries.add(new Entry(command, help));
        return this;
    }

    public String build() {
        StringBuilder sb = new StringBuilder();

        this.sort();
        int maxLength = this.getMaxSize();

        sb.append(this.title).append("\n");
        for (int i = 0; i < entries.size(); i++) {
            Entry entry = entries.get(i);

            sb.append(String.format(
                    leftPadding + "%-" + maxLength + "s    %s", entry.getCommand(), entry.getHelp()
            ));

            if (i < entries.size() - 1)
                sb.append("\n");
        }

        return sb.toString();
    }

    public void print() {
        System.out.println(this.build());
    }


    private void sort() {
        Collections.sort(entries);
    }


    private int getMaxSize() {
        Optional<Integer> max = this.entries.stream()
                .map(e -> e.getCommand().length())
                .max(Integer::compareTo);

        return max.orElse(0);
    }


    @AllArgsConstructor
    @Getter
    public static class Entry implements Comparable<Entry> {

        private String command;
        private String help;

        @Override
        public int compareTo(@NotNull HelpBuilder.Entry o) {
            return this.command.compareTo(o.command);
        }
    }

}
