package de.derteufelqwe.ServerManager.spring.commands;

import de.derteufelqwe.ServerManager.Docker;
import de.derteufelqwe.ServerManager.registry.DockerRegistryAPI;
import de.derteufelqwe.ServerManager.registry.objects.Catalog;
import de.derteufelqwe.ServerManager.registry.objects.ImageManifest;
import de.derteufelqwe.ServerManager.registry.objects.Tags;
import de.derteufelqwe.ServerManager.tablebuilder.Column;
import de.derteufelqwe.ServerManager.tablebuilder.TableBuilder;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.shell.standard.ShellCommandGroup;
import org.springframework.shell.standard.ShellComponent;
import org.springframework.shell.standard.ShellMethod;

import java.util.List;

@ShellComponent
@Log4j2
@ShellCommandGroup(value = "image")
public class ImageCommands {

    @Autowired
    private ApplicationEventPublisher applicationEventPublisher;
    @Autowired
    private Docker docker;
    @Autowired
    private StringRedisTemplate redisTemplate;
    @Autowired
    private DockerRegistryAPI registryAPI;


    @ShellMethod(value = "Lists all available images", key = "image list")
    public void listImages() {
        Catalog catalog = registryAPI.getCatalog();

        TableBuilder tableBuilder = new TableBuilder()
                .withColumn(new Column.Builder()
                        .withTitle("Name")
                        .build())
                .withColumn(new Column.Builder()
                        .withTitle("Tags")
                        .build());

        for (String image : catalog.getRepositories()) {
            Tags tags = registryAPI.getTags(image);
            List<String> firstTags = tags.getTags();
            int additionalTags = 0;

            // Trim the tags if there are too many
            if (tags.getTags().size() > 10) {
                firstTags = tags.getTags().subList(0, 10);
                additionalTags = tags.getTags().size() - 10;
            }

            tableBuilder.addToColumn(0, image);
            tableBuilder.addToColumn(1, String.join(", ", firstTags) + "  (" + additionalTags + " more)");
        }

        tableBuilder.build(log);
    }

    @ShellMethod(value = "Lists all tags for an image.", key = "image tags")
    public void imageTags(String imageName) {
        Tags tags = registryAPI.getTags(imageName);

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
