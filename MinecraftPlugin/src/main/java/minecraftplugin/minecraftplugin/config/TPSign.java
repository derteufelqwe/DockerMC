package minecraftplugin.minecraftplugin.config;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.bukkit.Location;
import org.bukkit.block.BlockState;
import org.bukkit.block.Sign;

import javax.annotation.Nullable;
import java.io.Serializable;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TPSign implements Serializable {

    private String name;
    private Location location;
    private String destination;

    @Nullable
    public Sign getSignBlock() {
        BlockState state = location.getWorld().getBlockAt(location).getState();

        if (state instanceof Sign) {
            return (Sign) state;
        }

        return null;
    }

}
