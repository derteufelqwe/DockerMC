package de.derteufelqwe.ServerManager.commands;

import de.derteufelqwe.ServerManager.utils.Pair;
import org.apache.commons.lang.StringUtils;
import picocli.CommandLine;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Shows basic help.
 */
@CommandLine.Command(name = "Help", description = "Shows a list of all available commandes.",
        mixinStandardHelpOptions = true)
public class HelpCommand implements Runnable {

    private List<Pair<String, String>> commandList = new ArrayList<>();

    public HelpCommand() {
        commandList.add(new Pair<>("Help", "Shows this help."));
        commandList.add(new Pair<>("Exit", "Gracefully quit ServerManager."));
        commandList.add(new Pair<>("Status", "General Network information."));
        commandList.add(new Pair<>("Docker", "Control over Docker."));
        commandList.add(new Pair<>("System", "Deep system control."));
        commandList.add(new Pair<>("Images", "Manage server images."));
    }

    @Override
    public void run() {
        int maxLength = commandList.stream().map(Pair::getFirst).max(Comparator.comparingInt(String::length)).get().length();

        System.out.println("Available commands:");
        for (Pair<String, String> p : commandList) {
            String workString = StringUtils.rightPad(p.getFirst(), maxLength, ' ');
            workString += " - " + p.getSecond();

            System.out.println(workString);
        }

    }


}
