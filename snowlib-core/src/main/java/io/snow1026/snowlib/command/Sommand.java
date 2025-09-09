package io.snow1026.snowlib.command;

import io.snow1026.snowlib.utils.reflect.ReflectFinder;
import org.bukkit.Bukkit;
import org.bukkit.command.*;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.util.*;
import java.util.function.Consumer;

/**
 * Central registry and executor for SnowLib commands.
 * <p>
 * Provides automatic registration of commands into Bukkit's {@link CommandMap}.
 * Each command is represented by a {@link CommandNode}, allowing nested arguments,
 * tab completion, permissions, and descriptions.
 * </p>
 *
 * <h2>Example</h2>
 * <pre>{@code
 * Sommand sommand = new Sommand(myPlugin);
 *
 * sommand.register("greet", root -> {
 *     root.arg(String.class, arg -> {
 *         arg.exec((ctx, args) -> {
 *             ctx.sender().sendMessage("Hello, " + ctx.arg(0) + "!");
 *         });
 *     });
 * });
 * }</pre>
 */
public class Sommand implements TabExecutor {

    private final JavaPlugin plugin;
    private final Map<String, CommandNode> commands = new HashMap<>();

    /**
     * Constructs a new {@link Sommand} instance bound to a plugin.
     *
     * @param plugin The owning plugin instance.
     */
    public Sommand(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    /**
     * Registers a command with a given name and configuration callback.
     *
     * @param name    Command name (case-insensitive).
     * @param builder Callback that configures the root {@link CommandNode}.
     */
    public void register(String name, Consumer<CommandNode> builder) {
        Objects.requireNonNull(name);
        Objects.requireNonNull(builder);

        CommandNode root = new CommandNode(String.class);
        builder.accept(root);
        commands.put(name.toLowerCase(Locale.ROOT), root);

        registerToBukkit(name);
    }

    /**
     * Registers the command into Bukkit's {@link CommandMap}.
     *
     * @param name Command name.
     */
    private void registerToBukkit(String name) {
        try {
            Field f = Bukkit.getServer().getClass().getDeclaredField("commandMap");
            f.setAccessible(true);
            CommandMap commandMap = (CommandMap) f.get(Bukkit.getServer());

            Constructor<PluginCommand> constructor =
                    ReflectFinder.findConstructor(PluginCommand.class, String.class, Plugin.class);
            PluginCommand cmd = constructor.newInstance(name, plugin);

            cmd.setExecutor(this);
            cmd.setTabCompleter(this);

            commandMap.register(plugin.getName(), cmd);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, Command command, @NotNull String label, String[] args) {
        CommandNode node = commands.get(command.getName().toLowerCase(Locale.ROOT));
        if (node == null) return false;
        return node.execute(sender, label, args, 0, new ArrayList<>());
    }

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, Command command, @NotNull String alias, String[] args) {
        CommandNode node = commands.get(command.getName().toLowerCase(Locale.ROOT));
        if (node == null) return Collections.emptyList();
        return node.suggest(sender, args, 0);
    }

    /**
     * Retrieves the {@link CommandNode} associated with a command.
     *
     * @param name Command name.
     * @return An {@link Optional} containing the node if found.
     */
    public Optional<CommandNode> getCommandNode(String name) {
        return Optional.ofNullable(commands.get(name.toLowerCase(Locale.ROOT)));
    }
}
