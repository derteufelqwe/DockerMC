package de.derteufelqwe.minecraftplugin.config;

import de.derteufelqwe.commons.docker.ServerName;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.bukkit.Location;
import org.bukkit.block.BlockState;
import org.bukkit.block.Sign;

import javax.annotation.Nullable;
import java.io.Serializable;

@Data
@NoArgsConstructor
public class TPSign implements Serializable {

    private String name;
    private Location location;
    private ServerName destination;

    private TPSignStatus status = TPSignStatus.ACTIVE;
    private short playerCount = 0;


    public TPSign(String name, Location location, ServerName destination) {
        this.name = name;
        this.location = location;
        this.destination = destination;
    }

    public TPSign(String name, Location location, String destination) {
        this(name, location, new ServerName(destination));
    }


    @Nullable
    public Sign getSignBlock() {
        BlockState state = location.getWorld().getBlockAt(location).getState();

        if (state instanceof Sign) {
            return (Sign) state;
        }

        return null;
    }

}
