package minecraftplugin.minecraftplugin.config;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.Synchronized;
import org.bukkit.Location;

import javax.annotation.Nullable;
import java.io.Serializable;
import java.util.*;
import java.util.stream.Collectors;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SignConfig implements Serializable {

    private List<TPSign> signs = new ArrayList<>();


    public void setup() {
        this.signs = Collections.synchronizedList(this.signs);
    }

    /**
     * Adds a new TPSign and overwrites any TPSigns at the current location
     * @param sign
     */
    public void addSign(TPSign sign) {
        Map<Location, TPSign> locMap = this.signs.stream().collect(
                Collectors.toMap(TPSign::getLocation, e -> e));

        if (locMap.containsKey(sign.getLocation())) {
            this.signs.remove(locMap.get(sign.getLocation()));
        }

        this.signs.add(sign);
    }

    public void removeSign(TPSign sign) {
        System.out.println("Removed: " + this.signs.remove(sign));
    }

    public boolean existsAt(Location location) {
        return this.signs.stream().map(TPSign::getLocation).collect(Collectors.toList()).contains(location);
    }

    @Nullable
    public TPSign getAt(Location location) {
        Map<Location, TPSign> locMap = this.signs.stream().collect(
                Collectors.toMap(TPSign::getLocation, e -> e));

        return locMap.getOrDefault(location, null);
    }

    @Nullable
    public TPSign getByServer(String name) {
        Map<String, TPSign> locMap = this.signs.stream().collect(
                Collectors.toMap(TPSign::getDestination, e -> e));

        return locMap.getOrDefault(name, null);
    }

    @Nullable
    public TPSign getByName(String name) {
        Map<String, TPSign> locMap = this.signs.stream().collect(
                Collectors.toMap(TPSign::getName, e -> e));

        return locMap.getOrDefault(name, null);
    }

}
