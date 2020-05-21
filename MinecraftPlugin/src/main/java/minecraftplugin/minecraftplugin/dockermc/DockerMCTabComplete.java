package minecraftplugin.minecraftplugin.dockermc;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class DockerMCTabComplete implements TabCompleter {

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] argsArray) {
        List<String> args = new ArrayList<>(Arrays.asList(argsArray));

        if (args.size() == 0) {
            return Arrays.asList("health", "version");

        } else {
            switch (args.get(0)) {
                case "health":
                    return this.forHealth(args.subList(1, args.size()));

                default:
                    return null;
            }
        }


    }

    private List<String> forHealth(List<String> args) {
        return Arrays.asList("on", "off");
    }

}
