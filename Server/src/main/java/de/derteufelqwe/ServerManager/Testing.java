package de.derteufelqwe.ServerManager;

import de.derteufelqwe.ServerManager.tablebuilder.Column;
import de.derteufelqwe.ServerManager.tablebuilder.TableBuilder;
import lombok.SneakyThrows;

public class Testing {

    @SneakyThrows
    public static void main(String[] args) {
        TableBuilder tableBuilder = new TableBuilder()
                .withBottomLine()
                .withColumn(new Column.Builder()
                        .withTitle("Title 1")
                        .withCell("Eintrag 1")
                        .withCell("Langer eintrag 2")
                        .build())
                .withColumn(new Column.Builder()
                        .withTitle("Title 2")
                        .withCell("E2.1")
                        .build())
                .withColumn(new Column.Builder()
                        .withTitle("ID")
                        .withCell("asldkfjaölsdkjföalsdkf")
                        .withCell("asldkfjaölsdkjföalsdkf")
                        .withCell("asldkfjaölsdkjföalsdkf")
                        .withCell("asldkfjaölsdkjföalsdkf")
                        .build());


        System.out.println(tableBuilder.build());

    }

}
