package de.derteufelqwe.commons.config.providers.deserializers;

import org.bukkit.Location;
import shaded.mcp.com.fasterxml.jackson.core.JsonParser;
import shaded.mcp.com.fasterxml.jackson.core.JsonProcessingException;
import shaded.mcp.com.fasterxml.jackson.databind.DeserializationContext;
import shaded.mcp.com.fasterxml.jackson.databind.deser.std.StdDeserializer;

import java.io.IOException;

public class LocationDeserializer extends StdDeserializer<Location> {

    protected LocationDeserializer() {
        super(Location.class);
    }

    @Override
    public Location deserialize(JsonParser jsonParser, DeserializationContext deserializationContext) throws IOException, JsonProcessingException {
        return null;
    }
}
