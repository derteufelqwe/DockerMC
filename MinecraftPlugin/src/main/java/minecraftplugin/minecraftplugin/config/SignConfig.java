package minecraftplugin.minecraftplugin.config;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.bukkit.Location;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SignConfig implements Serializable {

    private Map<Location, TPSign> signs = new HashMap<>();


    public void addSign(TPSign sign) {
        this.signs.put(sign.getLocation(), sign);
    }

    public void removeSign(TPSign sign) {
        if (this.signs.containsKey(sign.getLocation())) {
            this.signs.remove(sign.getLocation());
        }
    }

    public boolean exists(TPSign sign) {
        return this.signs.containsKey(sign.getLocation());
    }

    public TPSign get(Location location) {
        return this.signs.getOrDefault(location, null);
    }

}
