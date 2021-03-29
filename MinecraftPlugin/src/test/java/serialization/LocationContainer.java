package serialization;

import lombok.AllArgsConstructor;
import lombok.Data;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Data
@AllArgsConstructor
public class LocationContainer {

    private List<Location> locations = new ArrayList<>();
    private Map<Location, String> locationMap1 = new HashMap<>();
    private Map<String, Location> locationMap2 = new HashMap<>();

    public LocationContainer() {
        World world = Bukkit.getWorld("world");
        Location location1 = new Location(world, 1, 2, 3, 4, 5);
        Location location2 = new Location(world, 10, 20, 30, 40, 50);

        locations.add(location1);
        locations.add(location2);

        locationMap1.put(location1, "Location 1");
        locationMap1.put(location2, "Location 2");

        locationMap2.put("Location 1", location1);
        locationMap2.put("Location 2", location2);
    }

}
