package de.derteufelqwe.commons.config.providers.serializers;

import shaded.mcp.com.fasterxml.jackson.core.JsonGenerator;
import shaded.mcp.com.fasterxml.jackson.databind.JsonSerializer;
import shaded.mcp.com.fasterxml.jackson.databind.SerializerProvider;
import org.bukkit.Location;

import java.io.IOException;

public class LocationSerializer extends JsonSerializer<Location> {

    @Override
    public void serialize(Location value, JsonGenerator gen, SerializerProvider serializers) throws IOException {
        gen.writeStartObject();

        gen.writeObjectField("world", value.getWorld());
        gen.writeNumberField("x", value.getX());
        gen.writeNumberField("y", value.getY());
        gen.writeNumberField("z", value.getZ());
        gen.writeNumberField("yaw", value.getYaw());
        gen.writeNumberField("pitch", value.getPitch());

        gen.writeEndObject();
    }
}
