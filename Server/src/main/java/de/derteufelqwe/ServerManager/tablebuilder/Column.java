package de.derteufelqwe.ServerManager.tablebuilder;

import com.sun.istack.NotNull;
import lombok.Getter;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Getter
public class Column {

    private String title = "";
    private int withMin = 10;
    private int widthMax = -1;
    private String paddingLeft = " ";
    private String paddingRight = " ";

    private List<String> cells = new ArrayList<>();


    private Column() {

    }


    public void addCell(String data) {
        this.cells.add(data);
    }

    /**
     * Returns the the width of the row. The length of the individual cells and widthMax are taken into account aswell.
     */
    public int getWidth() {
        Optional<Integer> width = this.cells.stream().map(String::length).max(Integer::compareTo);
        if (width.isPresent() && width.get() > withMin)
            if (widthMax <= 0 || width.get() < widthMax)
                return width.get();
            else
                return widthMax;

        return withMin;
    }

    /**
     * Returns a cell or "" if it doesn't exist.
     * @return
     */
    @NotNull
    public String getCell(int index) {
        if (index >= cells.size())
            return "";

        String data = cells.get(index);
        if (widthMax > 0 && data.length() > widthMax)
            return data.substring(0, widthMax);

        return data;
    }


    public static class Builder {

        private Column column = new Column();


        public Builder() {

        }

        public Column build() {
            return this.column;
        }


        public Builder withTitle(String title) {
            this.column.title = title;
            return this;
        }

        public Builder withMinWidth(int minWidth) {
            this.column.withMin = minWidth;
            return this;
        }

        public Builder withMaxWidth(int maxWidth) {
            this.column.widthMax = maxWidth;
            return this;
        }

        public Builder withLeftPadding(String leftPadding) {
            this.column.paddingLeft = leftPadding;
            return this;
        }

        public Builder withRightPadding(String rightPadding) {
            this.column.paddingRight = rightPadding;
            return this;
        }

        public Builder withCell(String data) {
            this.column.addCell(data);
            return this;
        }

    }

}
