package de.derteufelqwe.nodewatcher.misc;

import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.config.plugins.Plugin;
import org.apache.logging.log4j.core.pattern.ConverterKeys;
import org.apache.logging.log4j.core.pattern.LogEventPatternConverter;

/**
 * Slices the beginning of the message away.
 */
@Plugin(name = "MyConverter", category = "Converter")
@ConverterKeys("smsg")
public class MyConverter extends LogEventPatternConverter {

    protected MyConverter(String name, String style) {
        super(name, style);
    }

    public static MyConverter newInstance(final String[] args) {
        return new MyConverter("test", "test");
    }

    @Override
    public void format(LogEvent event, StringBuilder toAppendTo) {
        String msg = event.getMessage().getFormattedMessage();

        if (msg.length() < 8)
            toAppendTo.append(msg);
        else
            toAppendTo.append(msg.substring(8));
    }
}
