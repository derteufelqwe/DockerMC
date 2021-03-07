package de.derteufelqwe.ServerManager.tablebuilder;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class TableBuilder {

    private String columnSeparator = "|";
    private String headerSeparator = "-";
    private String crossSeparator = "+";
    private String outerSeparator = "";
    private boolean bottomLine = false;

    private List<Column> columns = new ArrayList<>();


    public TableBuilder() {

    }

    private int getRowHeight(int rowIndex) {
        Optional<Integer> height = columns.stream()
                .map(c -> c.getCell(rowIndex))
                .map(Cell::getHeight)
                .max(Integer::compareTo);

        if (height.isPresent())
            return height.get();

        return 0;
    }

    /**
     * Builds a single row of the table
     * @param index Index of the row to build
     * @return
     */
    private String buildRow(int index) {
        StringBuilder sb = new StringBuilder();

        // All cells in that row
        List<Cell> cells = columns.stream()
                .map(c -> c.getCell(index))
                .collect(Collectors.toList());;

        Optional<Integer> maxheightOptional = cells.stream()
                .map(Cell::getHeight)
                .max(Integer::compareTo);

        if (!maxheightOptional.isPresent())
            return "";

        // The max height of all cells in that row
        int maxHeight = maxheightOptional.get();


        // Cell height iterator
        for (int i = 0; i < maxHeight; i++) {
            sb.append(outerSeparator);

            // Cell iterator
            for (int j = 0; j < cells.size(); j++) {
                Cell cell = cells.get(j);
                Column column = cell.getColumn();

                // Left padding
                sb.append(column.getPaddingLeft());

                // Entry
                sb.append(String.format("%-" + column.getWidth() + "s", cell.getLine(i)));

                // Right padding
                sb.append(column.getPaddingRight());

                // Cell separator
                if (j < columns.size() - 1) {
                    sb.append(columnSeparator);
                }
            }
            sb.append(outerSeparator);

            // Newline in the cell
            if (i < maxHeight - 1)
                sb.append("\n");
        }

        return sb.toString();
    }

    public String build() {
        StringBuilder sb = new StringBuilder();
        StringBuilder sep = new StringBuilder();

        // --- Build the heading ---
        sb.append(outerSeparator);  // Outer separator (left)
        if (!outerSeparator.equals(""))
            sep.append(crossSeparator);

        for (int i = 0; i < columns.size(); i++) {
            Column column = columns.get(i);

            // Left padding
            sb.append(column.getPaddingLeft());
            sep.append(StringUtils.repeat(headerSeparator, column.getPaddingLeft().length()));

            // Title
            sb.append(String.format("%-" + column.getWidth() + "s", column.getTitle()));
            sep.append(StringUtils.repeat(headerSeparator, column.getWidth()));

            // Right padding
            sb.append(column.getPaddingRight());
            sep.append(StringUtils.repeat(headerSeparator, column.getPaddingRight().length()));

            // Separator
            if (i < columns.size() - 1) {
                sb.append(columnSeparator);
                sep.append(crossSeparator);
            }
        }
        sb.append(outerSeparator);  // Outer separator (right)
        if (!outerSeparator.equals(""))
            sep.append(crossSeparator);

        // Heading separator line
        sb.append("\n");
        sb.append(sep);

        // --- Column entries ---

        Optional<Integer> longestColumn = this.columns.stream().map(c -> c.getCells().size()).max(Integer::compareTo);
        if (!longestColumn.isPresent())
            return sb.toString();


        // Column height iterator (row iterator)
        for (int i = 0; i < longestColumn.get(); i++) {
            sb.append("\n");

            sb.append(buildRow(i));

            // Separator line after row
            if (i < longestColumn.get() - 1) {
                sb.append("\n");
                sb.append(sep);
            }
        }

        // Last line
        if (bottomLine) {
            sb.append("\n");
            sb.append(sep);
        }

        return sb.toString();
    }

    public void build(Logger logger, Level level) {
        for (String line : build().split("\\n")) {
            logger.log(level, line);
        }
    }

    public void build(Logger logger) {
        build(logger, Level.INFO);
    }



    public void addToColumn(int index, String... lines) {
        if (columns.size() == 0)
            throw new RuntimeException("No columns added.");
        if (index > columns.size() - 1)
            throw new RuntimeException(String.format("Index %s too large. Only %s columns were added.", index, columns.size()));

        this.columns.get(index).addCell(String.join("\n", lines));
    }


    public TableBuilder withColumn(Column column) {
        this.columns.add(column);
        return this;
    }

    public TableBuilder withColumnSeparator(char separator) {
        this.columnSeparator = Character.toString(separator);
        return this;
    }

    public TableBuilder withHeaderSeparator(char separator) {
        this.headerSeparator = Character.toString(separator);
        return this;
    }

    public TableBuilder withCrossSeparator(char separator) {
        this.crossSeparator = Character.toString(separator);
        return this;
    }

    public TableBuilder withOuterSeparator(char separator) {
        this.outerSeparator = Character.toString(separator);
        return this;
    }

    public TableBuilder withBottomLine() {
        this.bottomLine = true;
        return this;
    }

}
