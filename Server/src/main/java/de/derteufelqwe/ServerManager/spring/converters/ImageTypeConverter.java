package de.derteufelqwe.ServerManager.spring.converters;

import com.sun.javaws.exceptions.InvalidArgumentException;
import de.derteufelqwe.ServerManager.spring.commands.ImageCommands;
import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;

@Component
public class ImageTypeConverter implements Converter<String, ImageCommands.ImageType> {

    @Override
    public ImageCommands.ImageType convert(String source) {
        source = source.toUpperCase();
        if (source.equals("MC"))
            source = "MINECRAFT";
        if (source.equals("BC"))
            source = "BUNGEE";

        try {
            return ImageCommands.ImageType.valueOf(source);

        } catch (IllegalArgumentException e) {
            return null;
        }
    }

}
