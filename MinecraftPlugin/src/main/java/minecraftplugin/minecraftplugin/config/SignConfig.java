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

    /**
     * Returns a TPSign for a given Location.
     * @return TPSign instance or null
     */
    @Nullable
    public TPSign getAt(Location location) {
        Map<Location, TPSign> locMap = this.signs.stream().collect(
                Collectors.toMap(TPSign::getLocation, e -> e));

        return locMap.getOrDefault(location, null);
    }

    /**
     * Returns all TPSigns, which connect to a certain server
     */
    public List<TPSign> getByServer(String name) {
        return this.signs.stream().filter(s -> s.getDestination().fullName().equals(name)).collect(Collectors.toList());
    }

    /**
     * Returns all TPSigns, which have a certain name
     */
    public List<TPSign> getByName(String name) {
        return this.signs.stream().filter(s -> s.getName().equals(name)).collect(Collectors.toList());
    }

    /**
     * Returns a list of TPSigns, which have the state active
     */
    public List<TPSign> getActiveSigns() {
        return this.signs.stream().filter(s -> s.getStatus() == TPSignStatus.ACTIVE).collect(Collectors.toList());
    }

    /**
     * Returns a list of TPSigns, which have the state restarting
     */
    public List<TPSign> getRestartingSigns() {
        return this.signs.stream().filter(s -> s.getStatus() == TPSignStatus.RESTARTING).collect(Collectors.toList());
    }

}
