package minecraftplugin.minecraftplugin;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

public class HealthCheckCommand implements CommandExecutor {

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 1) {
            if (args[0].equals("off")) {
                HealthHandler.healthy = false;
                System.out.println("Set container to unhealthy");

            } else {
                HealthHandler.healthy = true;
                System.out.println("Set container to healthy");
            }
        }

        return true;
    }
}
