package de.derteufelqwe.nodewatcher.logger;

import de.derteufelqwe.nodewatcher.NodeWatcher;
import de.derteufelqwe.nodewatcher.executors.ContainerEventHandler;
import de.derteufelqwe.nodewatcher.executors.NodeEventHandler;
import de.derteufelqwe.nodewatcher.executors.ServiceEventHandler;
import de.derteufelqwe.nodewatcher.executors.TimedPermissionWatcher;
import de.derteufelqwe.nodewatcher.health.ContainerHealthReader;
import de.derteufelqwe.nodewatcher.health.ServiceHealthReader;
import de.derteufelqwe.nodewatcher.stats.ContainerResourceWatcher;
import de.derteufelqwe.nodewatcher.stats.ContainerStatsCallback;
import de.derteufelqwe.nodewatcher.stats.HostResourceWatcher;
import lombok.Getter;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.config.plugins.Plugin;
import org.apache.logging.log4j.core.pattern.ConverterKeys;
import org.apache.logging.log4j.core.pattern.LogEventPatternConverter;

import java.util.HashMap;
import java.util.Map;

/**
 * Adds the prefix to a log message. The prefix identifies where the log messages was sent from
 */
@Plugin(name = "DMCPrefix", category = "Converter")
@ConverterKeys("dmcprefix")
public class DMCPrefix extends LogEventPatternConverter {

    @Getter
    private static final Map<String, String> prefixMap = new HashMap<>();

    static {
        prefixMap.put(NodeWatcher.class.getName(), "NodeW");
        prefixMap.put(ContainerEventHandler.class.getName(), "CW");
        prefixMap.put(ServiceEventHandler.class.getName(), "SW");
        prefixMap.put(TimedPermissionWatcher.class.getName(), "TPW");
        prefixMap.put(ContainerHealthReader.class.getName(), "CHealth");
        prefixMap.put(ServiceHealthReader.class.getName(), "SHealth");
        prefixMap.put(ContainerResourceWatcher.class.getName(), "CRW");
        prefixMap.put(ContainerStatsCallback.class.getName(), "Stats");
        prefixMap.put(HostResourceWatcher.class.getName(), "Stats");
        prefixMap.put(NodeEventHandler.class.getName(), "NodeEH");
    }

    public static int getMaxPrefixLength() {
        return prefixMap.values().stream()
                .map(String::length)
                .max(Integer::compareTo)
                .orElse(0);
    }


    private int maxPrefixLength = getMaxPrefixLength();


    protected DMCPrefix(String name, String style) {
        super(name, style);
    }

    public static DMCPrefix newInstance(final String[] args) {
        return new DMCPrefix("test", "test");
    }

    @Override
    public void format(LogEvent event, StringBuilder toAppendTo) {
        toAppendTo.append("[");
        toAppendTo.append(String.format("%-" + maxPrefixLength + "s", prefixMap.getOrDefault(event.getLoggerName(), "None")));
        toAppendTo.append("]");
    }
}
