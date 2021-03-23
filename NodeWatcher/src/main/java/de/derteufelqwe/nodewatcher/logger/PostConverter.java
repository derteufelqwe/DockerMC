package de.derteufelqwe.nodewatcher.logger;

import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.config.plugins.Plugin;
import org.apache.logging.log4j.core.pattern.ConverterKeys;
import org.apache.logging.log4j.core.pattern.LogEventPatternConverter;

/**
 * Returns the message without the prefix
 */
@Plugin(name = "DMCPostConverter", category = "Converter")
@ConverterKeys("dmcpost")
public class PostConverter extends LogEventPatternConverter {

    protected PostConverter(String name, String style) {
        super(name, style);
    }

    public static PostConverter newInstance(final String[] args) {
        return new PostConverter("test", "test");
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
