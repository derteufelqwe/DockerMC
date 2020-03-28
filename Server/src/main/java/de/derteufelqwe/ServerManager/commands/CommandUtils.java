package de.derteufelqwe.ServerManager.commands;

import de.derteufelqwe.ServerManager.utils.Pair;
import org.apache.commons.lang.StringUtils;
import picocli.CommandLine;

import java.util.Comparator;
import java.util.Map;

public class CommandUtils {

    public static String getSubcommandsHelp(Runnable instance) {
        String string = "Available subcommands are:";
        Map<String, CommandLine.Help> subcommands = new CommandLine(instance).getHelp().subcommands();

        int maxLength = subcommands.keySet().stream().max(Comparator.comparingInt(String::length)).get().length();

        for (Map.Entry<String, CommandLine.Help> entry : subcommands.entrySet()) {
            string += "\n  " + StringUtils.rightPad(entry.getKey(), maxLength, ' ') + " - " + entry.getValue().description();
        }

        return string;
    }

}
