package minecraftplugin.minecraftplugin.config;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.bukkit.Location;

import java.io.Serializable;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TPSign implements Serializable {

    private String name;
    private Location location;
    private String destination;

}
