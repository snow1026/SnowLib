package io.github.snow1026.snowlib.commands;

import io.github.snow1026.snowlib.Snow;
import io.github.snow1026.snowlib.exceptions.CommandParseException;
import io.github.snow1026.snowlib.commands.handler.DefaultExceptionHandler;
import io.github.snow1026.snowlib.commands.handler.ExceptionHandler;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandMap;
import org.bukkit.command.CommandSender;
import org.bukkit.command.PluginCommand;
import org.bukkit.command.TabExecutor;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.util.*;

/**
 * The central registry and executor for SnowLib commands.
 * <p>
 * This class handles the automatic registration of commands into Bukkit's {@link CommandMap}
 * and provides a fluent, builder-style API for defining complex command structures with ease.
 *
 * <h2>Key Features</h2>
 * <ul>
 * <li><b>Fluent Builder:</b> Intuitively construct commands via method chaining, starting with {@link #register(String)}.</li>
 * <li><b>Automatic Registration:</b> Simplifies setup by automatically registering commands with Bukkit.</li>
 * <li><b>Custom Exception Handling:</b> Allows for customized handling of permissions failure, argument parsing errors, and more via {@link #setExceptionHandler(ExceptionHandler)}.</li>
 * </ul>
 *
 * <h2>Example Usage</h2>
 * <pre>{@code
 * Sommand sommand = new Sommand(myPlugin);
 *
 * sommand.register("greet")
 * .argument("target", Player.class, targetNode ->
 * targetNode.executes(ctx -> {
 * Player target = ctx.getArgument("target");
 * ctx.sender().sendMessage("Hello, " + target.getName() + "!");
 * })
 * )
 * .executes(ctx -> {
 * ctx.sender().sendMessage("Usage: /greet <player>");
 * });
 * }</pre>
 *
 * @see SommandNode
 * @see SommandContext
 */
public class Sommand extends Snow implements TabExecutor {

    private final JavaPlugin plugin;
    private final Map<String, SommandNode> rootCommands = new HashMap<>();
    private ExceptionHandler exceptionHandler = new DefaultExceptionHandler();

    /**
     * Constructs a new Sommand instance for the given plugin.
     *
     * @param plugin The plugin instance that owns these commands.
     */
    public Sommand(@NotNull JavaPlugin plugin) {
        this.plugin = Objects.requireNonNull(plugin, "Plugin cannot be null");
    }

    /**
     * Registers a new top-level command and returns its root node for configuration.
     *
     * @param name The name of the command (case-insensitive).
     * @return The root {@link SommandNode} for fluent command construction.
     */
    public SommandNode register(@NotNull String name) {
        Objects.requireNonNull(name, "Command name cannot be null");
        String lowerName = name.toLowerCase(Locale.ROOT);

        SommandNode root = new SommandNode(lowerName, String.class);
        this.rootCommands.put(lowerName, root);

        registerToBukkit(name, lowerName);
        return root;
    }

    /**
     * Sets a custom handler for exceptions that occur during command parsing or execution.
     *
     * @param handler The exception handler to use. If null, the default handler will be used.
     */
    public void setExceptionHandler(@NotNull ExceptionHandler handler) {
        this.exceptionHandler = Objects.requireNonNull(handler, "Exception handler cannot be null");
    }

    // Sommand.java의 registerToBukkit 메서드 내부
    private void registerToBukkit(String name, String lowerName) {
        try {
            Field commandMapField = Bukkit.getServer().getClass().getDeclaredField("commandMap");
            commandMapField.setAccessible(true);
            CommandMap commandMap = (CommandMap) commandMapField.get(Bukkit.getServer());

            // 기존 커맨드를 가져옵니다.
            Command existing = commandMap.getCommand(lowerName);

            // instanceof로 PluginCommand 타입인지 먼저 확인합니다.
            if (existing instanceof PluginCommand existingPluginCommand) {
                // 같은 Sommand 인스턴스가 이미 등록했는지 확인합니다.
                if (existingPluginCommand.getExecutor() == this) {
                    return; // 이미 등록되었으므로 중단
                }
            }

            // 아래는 기존 등록 로직입니다.
            Constructor<PluginCommand> constructor = PluginCommand.class.getDeclaredConstructor(String.class, Plugin.class);
            constructor.setAccessible(true);
            PluginCommand cmd = constructor.newInstance(name, plugin);

            cmd.setExecutor(this);
            cmd.setTabCompleter(this);

            commandMap.register(plugin.getName().toLowerCase(Locale.ROOT), cmd);
        } catch (Exception e) {
            throw new RuntimeException("Failed to register command '" + name + "'", e);
        }
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        SommandNode root = rootCommands.get(command.getName().toLowerCase(Locale.ROOT));
        if (root == null) return false;

        try {
            root.execute(sender, label, args);
        } catch (CommandParseException e) {
            exceptionHandler.handle(sender, e);
        } catch (Exception e) {
            exceptionHandler.handle(sender, new CommandParseException("An unexpected error occurred.", e));
        }
        return true;
    }

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, @NotNull String[] args) {
        SommandNode root = rootCommands.get(command.getName().toLowerCase(Locale.ROOT));
        if (root == null) return Collections.emptyList();
        try {
            return root.suggest(sender, args);
        } catch (Exception e) {
            // Suppress exceptions during tab completion to prevent console spam
            return Collections.emptyList();
        }
    }
}
