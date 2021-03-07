package de.derteufelqwe.ServerManager;

import de.derteufelqwe.ServerManager.registry.DockerRegistryAPI;
import de.derteufelqwe.ServerManager.tablebuilder.Column;
import de.derteufelqwe.ServerManager.tablebuilder.TableBuilder;
import lombok.SneakyThrows;
import org.fusesource.jansi.Ansi;
import org.fusesource.jansi.AnsiConsole;

public class Testing {

    @SneakyThrows
    public static void main(String[] args) {

        TableBuilder tableBuilder = new TableBuilder()
//                .withOuterSeparator('|')
//                .withBottomLine()
                .withColumn(new Column.Builder()
                        .withTitle("Title")
                        .build())
                .withColumn(new Column.Builder()
                        .withTitle("Content")
                        .build());

        tableBuilder.addToColumn(0, "A");
        tableBuilder.addToColumn(1, "1", "2");

        tableBuilder.addToColumn(0, "B\n\nYo");
        tableBuilder.addToColumn(1, "1\n2\n3\n4");

        tableBuilder.addToColumn(1, "1\n2");

        System.out.println(tableBuilder.build());
    }

}
