package de.derteufelqwe.ServerManager.tablebuilder;

import com.sun.istack.NotNull;
import lombok.Getter;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

public class Cell {

    private Column column;
    private List<String> lines = new ArrayList<>();


    public Cell(String content, Column column) {
        this.column = column;

        if (content == null)
            lines.add("null");
        else
            lines.addAll(Arrays.asList(content.split("\\n")));
    }


    public int getHeight() {
        return this.lines.size();
    }

    public int getMaxWitdh() {
        Optional<Integer> value = lines.stream().map(String::length).max(Integer::compareTo);
        if (value.isPresent())
            return value.get();

        return 0;
    }

    public Column getColumn() {
        return this.column;
    }

    /**
     * Returns a line or "" if it doesn't exist.
     * @return
     */
    @NotNull
    public String getLine(int index) {
        int widthMax = column.getWidthMax();
        if (index >= lines.size())
            return "";

        String data = lines.get(index);
        if (widthMax > 0 && data.length() > widthMax)
            return data.substring(0, widthMax);

        return data;
    }

    @Override
    public String toString() {
        return String.format("Cell<%s>", String.join("\n", lines));
    }
}
