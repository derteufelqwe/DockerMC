package de.derteufelqwe.commons.config.providers.serializers;

import shaded.mcp.com.fasterxml.jackson.core.JsonGenerator;
import shaded.mcp.com.fasterxml.jackson.databind.SerializerProvider;
import shaded.mcp.com.fasterxml.jackson.databind.ser.std.StdSerializer;
import org.bukkit.World;

import java.io.IOException;

public class WorldSerializer extends StdSerializer<World> {

    public WorldSerializer() {
        super(World.class);
    }

    @Override
    public void serialize(World value, JsonGenerator gen, SerializerProvider serializers) throws IOException {
//        gen.writeStartObject();

        gen.writeString(value.getUID().toString());

//        gen.writeEndObject();
    }
}
