package de.derteufelqwe.ServerManager.cli;


import de.derteufelqwe.ServerManager.cli.image.BuildImageCmd;
import de.derteufelqwe.ServerManager.cli.image.DeleteImageCmd;
import de.derteufelqwe.ServerManager.cli.image.ListImagesCmd;
import de.derteufelqwe.ServerManager.cli.image.ShowImageTagsCmd;
import lombok.Getter;
import lombok.extern.log4j.Log4j2;
import picocli.CommandLine;

@CommandLine.Command(name = "image", subcommands = {HelpCommand.class,
        ListImagesCmd.class, ShowImageTagsCmd.class, DeleteImageCmd.class, BuildImageCmd.class
})
@Log4j2
public class ImageCmd implements Runnable {

    @Getter
    @CommandLine.ParentCommand
    private CliCommands parent;

    @Override
    public void run() {
        System.out.println(new CommandLine(this).setUsageHelpAutoWidth(true).getUsageMessage());
    }
}
