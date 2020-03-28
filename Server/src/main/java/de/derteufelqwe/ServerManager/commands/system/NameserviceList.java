package de.derteufelqwe.ServerManager.commands.system;

import de.derteufelqwe.ServerManager.setup.BindConfigurator;
import de.derteufelqwe.ServerManager.utils.Pair;
import org.apache.commons.lang.StringUtils;
import picocli.CommandLine;

import java.util.List;

/**
 * List the DNS entries
 */
@CommandLine.Command(name = "list", description = "List the user defined DNS entries.",
        mixinStandardHelpOptions = true, subcommands = {

})
public class NameserviceList implements Runnable {

    @CommandLine.Option(names = {"-a", "--all"}, description = "Also show system defined (not editable) dns entries.")
    private boolean showAll = false;

    private BindConfigurator bindConfigurator = new BindConfigurator();

    @Override
    public void run() {
        List<Pair<String, String>> entries = bindConfigurator.getEntriesAsList(BindConfigurator.Type.USER);
        if (showAll) {
            entries.addAll(bindConfigurator.getEntriesAsList(BindConfigurator.Type.SYSTEM));
        }

        String text = "DNS entries:\n";
        for (Pair<String, String> entry : entries) {
            text += StringUtils.rightPad(entry.getFirst(), 25, " ") + entry.getSecond() + "\n";
        }

        System.out.println(text);
    }

}
