package de.derteufelqwe.ServerManager.cli.converters;

import picocli.CommandLine;

import java.time.Duration;

public class DurationConverter implements CommandLine.ITypeConverter<Duration> {

    @Override
    public Duration convert(String value) throws Exception {
        if (value.toLowerCase().endsWith("d")) {
            return Duration.parse("P" + value);
        }

        return Duration.parse("PT" + value);
    }
}
