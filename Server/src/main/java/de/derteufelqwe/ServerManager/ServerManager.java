package de.derteufelqwe.ServerManager;

import de.derteufelqwe.ServerManager.cli.CliCommands;
import de.derteufelqwe.ServerManager.cli.converters.DurationConverter;
import de.derteufelqwe.ServerManager.config.MainConfig;
import de.derteufelqwe.ServerManager.config.OldServersConfig;
import de.derteufelqwe.ServerManager.config.ServersConfig;
import de.derteufelqwe.ServerManager.registry.DockerRegistryAPI;
import de.derteufelqwe.ServerManager.utils.Commons;
import de.derteufelqwe.commons.Constants;
import de.derteufelqwe.commons.config.Config;
import de.derteufelqwe.commons.config.providers.DefaultGsonProvider;
import de.derteufelqwe.commons.config.providers.DefaultYamlConverter;
import de.derteufelqwe.commons.hibernate.SessionBuilder;
import de.derteufelqwe.commons.redis.RedisPool;
import lombok.Getter;
import lombok.extern.log4j.Log4j2;
import org.fusesource.jansi.AnsiConsole;
import org.jline.console.SystemRegistry;
import org.jline.console.impl.Builtins;
import org.jline.console.impl.SystemRegistryImpl;
import org.jline.keymap.KeyMap;
import org.jline.reader.*;
import org.jline.reader.impl.DefaultParser;
import org.jline.terminal.Size;
import org.jline.terminal.Terminal;
import org.jline.terminal.TerminalBuilder;
import org.jline.widget.TailTipWidgets;
import picocli.CommandLine;
import picocli.shell.jline3.PicocliCommands;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.function.Supplier;


@Log4j2
public class ServerManager {

    /*
     * ToDo-List:
     *  - Docker secrets for password management
     *  - Improved config system
     *  - Clean database layouts
     *  - Switch overlay network driver
     *  - Automatic MC / BC plugin downloading
     *  - Tag DockerMC application and BC / MC plugins with a database schema version
     *  - Check that the plugins have correct names on image build
     *  - Make sure that the NodeWatcher has created the service before containers get created
     *  - Encrypt Registry SSL certificate
     *  - Track Registry ssl certificate age
     *  - Refactor the server creation / update methods
     */

    /*
     * Exit codes:
     *  100: Infrastructure setup failed
     *  101: Minecraft server setup failed
     *  102: Invalid server config file
     */

    @Getter
    public static final Config<MainConfig> mainConfig = new Config<>(new DefaultYamlConverter(), new DefaultGsonProvider(), Constants.CONFIG_PATH + "/main.yml", new MainConfig());
    @Getter
    public static final Config<ServersConfig> serverConfig = new Config<>(new DefaultYamlConverter(), new DefaultGsonProvider(), Constants.CONFIG_PATH + "/servers.yml", new ServersConfig());
    @Getter
    public static final Config<OldServersConfig> serverConfigOld = new Config<>(new DefaultYamlConverter(), new DefaultGsonProvider(), Constants.DATA_PATH + "/servers_old.yml", new OldServersConfig());

    @Getter private static SessionBuilder sessionBuilder;
    @Getter private static Docker docker;
    @Getter private static Commons commons;
    @Getter private static DockerRegistryAPI registryAPI;
    @Getter private static RedisPool redisPool;


    public static void main(String[] args) {
        mainConfig.load();
        serverConfigOld.load();
        serverConfig.load();
        log.info("Connecting to the database...");
        sessionBuilder = new SessionBuilder(Constants.DB_DMC_USER, "admin", Constants.DMC_MASTER_DNS_NAME);
        log.info("Connecting to the docker engine...");
        docker = new Docker("tcp", "ubuntu1", 2375, mainConfig.get());
        registryAPI = new DockerRegistryAPI("https://" + Constants.REGISTRY_URL, mainConfig.get().getRegistryUsername(), mainConfig.get().getRegistryPassword());
        redisPool = new RedisPool("ubuntu1");
        commons = new Commons();

        try {
            startCLI();

        } finally {
            try {
                docker.close();

            } catch (IOException e) {
                log.error("Closing docker connection failed.", e);
            }
            sessionBuilder.close();
        }
    }

    private static void startCLI() {
        AnsiConsole.systemInstall();
        try {
            Supplier<Path> workDir = () -> Paths.get(System.getProperty("user.dir"));

            // Set up JLine built-in commands
            Builtins builtins = new Builtins(workDir, null, null);
            builtins.rename(Builtins.Command.TTOP, "top");
            builtins.alias("zle", "widget");
            builtins.alias("bindkey", "keymap");

            // Set up picocli commands
            CliCommands commands = new CliCommands();
            PicocliCommands.PicocliCommandsFactory factory = new PicocliCommands.PicocliCommandsFactory();
            // Or, if you have your own factory, you can chain them like this:
            // MyCustomFactory customFactory = createCustomFactory(); // your application custom factory
            // PicocliCommandsFactory factory = new PicocliCommandsFactory(customFactory); // chain the factories

            CommandLine cmd = new CommandLine(commands, factory);
            cmd.registerConverter(Duration.class, new DurationConverter());
            PicocliCommands picocliCommands = new PicocliCommands(cmd);

            Parser parser = new DefaultParser();
            try (Terminal terminal = TerminalBuilder.builder().size(new Size(10, 20)).build()) {
                SystemRegistry systemRegistry = new SystemRegistryImpl(parser, terminal, workDir, null);
                systemRegistry.setCommandRegistries(builtins, picocliCommands);
                systemRegistry.register("help", picocliCommands);

                LineReader reader = LineReaderBuilder.builder()
                        .terminal(terminal)
                        .completer(systemRegistry.completer())
                        .parser(parser)
                        .variable(LineReader.LIST_MAX, 50)   // max tab completion candidates
                        .build();
                builtins.setLineReader(reader);
                commands.setLineReader(reader);
                factory.setTerminal(terminal);

                TailTipWidgets widgets = new TailTipWidgets(reader, systemRegistry::commandDescription, 5, TailTipWidgets.TipType.COMPLETER);
                widgets.enable();
                KeyMap<Binding> keyMap = reader.getKeyMaps().get("main");
                keyMap.bind(new Reference("tailtip-toggle"), KeyMap.alt("s"));

                String prompt = "dmc> ";
                String rightPrompt = null;

                // start the shell and process input until the user quits with Ctrl-D
                String line;
                while (true) {
                    try {
                        systemRegistry.cleanUp();
                        line = reader.readLine(prompt, rightPrompt, (MaskingCallback) null, null);
                        systemRegistry.execute(line);
                    } catch (UserInterruptException e) {
                        // Ignore
                    } catch (EndOfFileException e) {
                        return;
                    } catch (Exception e) {
                        systemRegistry.trace(e);
                    }
                }
            }

        } catch (Throwable t) {
            t.printStackTrace(System.err);

        } finally {
            AnsiConsole.systemUninstall();
        }
    }

}
