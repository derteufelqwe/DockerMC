package de.derteufelqwe.ServerManager.cli.image;

import de.derteufelqwe.ServerManager.ServerManager;
import de.derteufelqwe.ServerManager.registry.DockerRegistryAPI;
import de.derteufelqwe.ServerManager.registry.objects.Catalog;
import de.derteufelqwe.ServerManager.registry.objects.ImageManifest;
import de.derteufelqwe.ServerManager.registry.objects.Tags;
import de.derteufelqwe.ServerManager.tablebuilder.Column;
import de.derteufelqwe.ServerManager.tablebuilder.TableBuilder;
import lombok.extern.log4j.Log4j2;
import picocli.CommandLine;

import java.util.List;

@CommandLine.Command(name = "list", description = "Lists all available images")
@Log4j2
public class ListImagesCmd implements Runnable {

    private DockerRegistryAPI registryAPI = ServerManager.getRegistryAPI();


    @CommandLine.Option(names = {"-nb", "--no-bungee"}, description = "Hide BungeeCord images")
    private boolean noBungee = false;

    @CommandLine.Option(names = {"-nm", "--no-minecraft"}, description = "Hide Minecraft images")
    private boolean noMinecraft = false;


    @Override
    public void run() {
        Catalog catalog = registryAPI.getCatalog();

        TableBuilder tableBuilder = new TableBuilder()
                .withNoRowSeparation()
                .withColumn(new Column.Builder()
                        .withTitle("Name")
                        .build())
                .withColumn(new Column.Builder()
                        .withTitle("Type")
                        .build())
                .withColumn(new Column.Builder()
                        .withTitle("Tags")
                        .build());

        for (String image : catalog.getRepositories()) {
            Tags tags = registryAPI.getTags(image);
            List<String> firstTags = tags.getTags();
            int additionalTags = 0;

            if (tags.getTags().size() == 0)
                continue;

            // Trim the tags if there are too many
            if (tags.getTags().size() > 10) {
                firstTags = tags.getTags().subList(0, 10);
                additionalTags = tags.getTags().size() - 10;
            }

            // Skip images based on the filter
            ImageManifest manifest = registryAPI.getManifest(image, firstTags.get(0));
            String type = manifest.getDMCImageType();

            if (noMinecraft && type != null && type.equals(BuildImageCmd.ImageType.MINECRAFT.name()))
                continue;
            if (noBungee && type != null && type.equals(BuildImageCmd.ImageType.BUNGEE.name()))
                continue;

            tableBuilder.addToColumn(0, image);
            tableBuilder.addToColumn(1, type);
            tableBuilder.addToColumn(2, String.join(", ", firstTags) + "  (" + additionalTags + " more)");
        }

        tableBuilder.build(log);
    }
}
