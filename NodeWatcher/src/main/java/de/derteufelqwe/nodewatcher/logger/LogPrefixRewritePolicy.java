package de.derteufelqwe.nodewatcher.logger;

import de.derteufelqwe.nodewatcher.NodeWatcher;
import de.derteufelqwe.nodewatcher.executors.ContainerWatcher;
import de.derteufelqwe.nodewatcher.executors.ServiceWatcher;
import de.derteufelqwe.nodewatcher.executors.TimedPermissionWatcher;
import de.derteufelqwe.nodewatcher.health.ContainerHealthReader;
import de.derteufelqwe.nodewatcher.health.ServiceHealthReader;
import lombok.Getter;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.appender.rewrite.RewritePolicy;
import org.apache.logging.log4j.core.config.plugins.Plugin;
import org.apache.logging.log4j.core.config.plugins.PluginFactory;

import java.util.HashMap;
import java.util.Map;

@Plugin(name = "LogPrefixRewritePolicy", category = "Core", elementType = "rewritePolicy", printObject = true)
public final class LogPrefixRewritePolicy implements RewritePolicy {

    @Getter
    private static final Map<String, String> prefixMap = new HashMap<>();

    static {
        prefixMap.put(NodeWatcher.class.getName(), "NW");
        prefixMap.put(ContainerWatcher.class.getName(), "CW");
        prefixMap.put(ServiceWatcher.class.getName(), "SW");
        prefixMap.put(TimedPermissionWatcher.class.getName(), "TPW");
        prefixMap.put(ContainerHealthReader.class.getName(), "CHealth");
        prefixMap.put(ServiceHealthReader.class.getName(), "SHealth");
    }

    public static int getMaxPrefixLength() {
        return prefixMap.values().stream()
                .map(String::length)
                .max(Integer::compareTo)
                .orElse(0);
    }


    @Override
    public LogEvent rewrite(LogEvent source) {
        return source;
    }


    @PluginFactory
    public static LogPrefixRewritePolicy createPolicy() {
        return new LogPrefixRewritePolicy();
    }

}
