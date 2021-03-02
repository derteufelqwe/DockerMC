package de.derteufelqwe.ServerManager.spring.commands;

import com.github.dockerjava.api.command.CreateContainerResponse;
import com.github.dockerjava.api.command.WaitContainerResultCallback;
import com.github.dockerjava.api.model.Bind;
import com.github.dockerjava.api.model.Volume;
import de.derteufelqwe.ServerManager.Docker;
import de.derteufelqwe.ServerManager.ServerManager;
import de.derteufelqwe.ServerManager.config.objects.CertificateCfg;
import de.derteufelqwe.ServerManager.registry.DockerRegistryAPI;
import de.derteufelqwe.ServerManager.registry.objects.*;
import de.derteufelqwe.ServerManager.tablebuilder.Column;
import de.derteufelqwe.ServerManager.tablebuilder.TableBuilder;
import de.derteufelqwe.commons.Constants;
import de.derteufelqwe.commons.Utils;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.shell.standard.ShellCommandGroup;
import org.springframework.shell.standard.ShellComponent;
import org.springframework.shell.standard.ShellMethod;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

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
    @Autowired(required = false)
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

    /*
     * Base path: /var/lib/registry/docker/registry/v2/repositories
     */

    @ShellMethod(value = "Deletes an image from the registry.", key = "image delete")
    public void deleteImage(String image) {
        String tag = "latest";
        Tags tags = registryAPI.getTags(image);
        System.out.println(tags);
        ImageManifest manifest = registryAPI.getManifest(image, tag);
        System.out.println(manifest.getContentDigest());
        DeleteManifest deleteManifest = registryAPI.getDeleteManifest(image, tag);
        System.out.println(deleteManifest.getContentDigest());

        registryAPI.deleteManifest(image, deleteManifest.getContentDigest());

        System.out.println(registryAPI.getTags(image));
    }

    /*
     *  docker run --rm --network overnet lhanxetus/deckschrubber -registry https://registry:5000 -username admin -password root -repos testmc -latest 0 -debug -insecure
     *  openssl x509 -in ca.crt -noout -text
     */

    @ShellMethod(value = "testing", key = "test")
    public void test() {
        CertificateCfg cfg = ServerManager.MAIN_CONFIG.get().getRegistryCerCfg();

        // -----  SSL-certificate generation  -----

        Volume sslOutput = new Volume("/export");
        List<String> command = new ArrayList<>(Arrays.asList(
                "req", "-newkey", "rsa:4096", "-nodes", "-sha256", "-x509", "-days", "356",
                "-out", "/export/" + Constants.REGISTRY_CERT_NAME, "-keyout", "/export/" + Constants.REGISTRY_KEY_NAME,
                "-config", "/export/domain.cnf"));

//        docker.pullImage(Constants.Images.OPENSSL.image());
        CreateContainerResponse response = docker.getDocker().createContainerCmd(Constants.Images.OPENSSL.image())
                .withLabels(Utils.quickLabel(Constants.ContainerType.REGISTRY_CERTS_GEN))
                .withVolumes(sslOutput)
                .withBinds(new Bind(Constants.REGISTRY_CERT_PATH_2, sslOutput))
                .withCmd(command)
                .exec();

        System.out.println("Id: " + response.getId());
        docker.getDocker().startContainerCmd(response.getId()).exec();

        docker.getDocker().waitContainerCmd(response.getId())
                .exec(new WaitContainerResultCallback())
                .awaitStatusCode(10, TimeUnit.SECONDS);

        System.out.println("Done");
    }

}
