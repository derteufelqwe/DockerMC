package de.derteufelqwe.ServerManager.tablebuilder;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.Logger;
import org.springframework.boot.orm.jpa.hibernate.SpringImplicitNamingStrategy;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class TableBuilder {

    private String columnSeparator = "|";
    private String headerSeparator = "-";
    private String headerCrossSymbol = "+";
    private String outerSeparator = "";
    private boolean bottomLine = false;

    private List<Column> columns = new ArrayList<>();


    public TableBuilder() {

    }

    public String build() {
        StringBuilder sb = new StringBuilder();
        StringBuilder sep = new StringBuilder();

        sb.append(outerSeparator);
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
                sep.append(headerCrossSymbol);
            }
        }
        // Separator line
        sb.append("\n");
        sb.append(sep);

        // --- Column entries ---

        Optional<Integer> longestColumn = this.columns.stream().map(c -> c.getCells().size()).max(Integer::compareTo);
        if (!longestColumn.isPresent())
            return sb.toString();


        for (int i = 0; i < longestColumn.get(); i++) {
            sb.append("\n");

            for (int j = 0; j < columns.size(); j++) {
                Column column = columns.get(j);

                // Left padding
                sb.append(column.getPaddingLeft());

                // Entry
                sb.append(String.format("%-" + column.getWidth() + "s", column.getCell(i)));

                // Right padding
                sb.append(column.getPaddingRight());

                // Separator
                if (j < columns.size() - 1) {
                    sb.append(columnSeparator);
                }
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



    public void addToColumn(int index, String data) {
        if (columns.size() == 0)
            throw new RuntimeException("No columns added.");
        if (index > columns.size() - 1)
            throw new RuntimeException(String.format("Index %s too large. Only %s columns were added.", index, columns.size()));

        this.columns.get(index).addCell(data);
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

    public TableBuilder withHeaderCrossSeparator(char separator) {
        this.headerCrossSymbol = Character.toString(separator);
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
