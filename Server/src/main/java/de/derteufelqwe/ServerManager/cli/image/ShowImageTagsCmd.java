package de.derteufelqwe.ServerManager.cli.image;

import de.derteufelqwe.ServerManager.ServerManager;
import de.derteufelqwe.ServerManager.registry.DockerRegistryAPI;
import de.derteufelqwe.ServerManager.registry.RegistryAPIException;
import de.derteufelqwe.ServerManager.registry.objects.ImageManifest;
import de.derteufelqwe.ServerManager.registry.objects.Tags;
import de.derteufelqwe.ServerManager.tablebuilder.Column;
import de.derteufelqwe.ServerManager.tablebuilder.TableBuilder;
import lombok.extern.log4j.Log4j2;
import picocli.CommandLine;

@CommandLine.Command(name = "tags", description = "Lists all tags / versions of an image")
@Log4j2
public class ShowImageTagsCmd implements Runnable {

    private DockerRegistryAPI registryAPI = ServerManager.getRegistryAPI();


    @CommandLine.Parameters(description = "Name of the image")
    private String imageName;


    @Override
    public void run() {
        Tags tags;
        try {
            tags = registryAPI.getTags(imageName);
            if (tags.getTags().size() == 0)
                throw new RegistryAPIException("");

        } catch (RegistryAPIException e) {
            log.error("Image '{}' not found in registry.", imageName);
            return;
        }

        TableBuilder tableBuilder = new TableBuilder()
                .withColumn(new Column.Builder()
                        .withTitle("Tag")
                        .build())
                .withColumn(new Column.Builder()
                        .withTitle("Last modified")
                        .build());

        for (String tag : tags.getTags()) {
            ImageManifest manifest = registryAPI.getManifest(imageName, tag);

            tableBuilder.addToColumn(0, tag);
            tableBuilder.addToColumn(1, manifest.getLastModified().toString());
        }

        tableBuilder.build(log);
    }
}
