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
     * <p>
     * This node can be used to set properties like aliases, description, and usage messages
     * for the command, in addition to defining its sub-commands and execution logic.
     *
     * @param name The name of the command (case-insensitive).
     * @return The root {@link SommandNode} for fluent command construction.
     */
    public SommandNode register(@NotNull String name) {
        Objects.requireNonNull(name, "Command name cannot be null");
        String lowerName = name.toLowerCase(Locale.ROOT);

        PluginCommand cmd = getOrCreateCommand(name);
        SommandNode root = new SommandNode(lowerName, String.class, cmd);

        this.rootCommands.put(lowerName, root);

        cmd.setExecutor(this);
        cmd.setTabCompleter(this);
        
        return root;
    }

    /**
     * Sets a custom handler for exceptions that occur during command parsing or execution.
     *
     * @param handler The exception handler to use.
     */
    public void setExceptionHandler(@NotNull ExceptionHandler handler) {
        this.exceptionHandler = Objects.requireNonNull(handler, "Exception handler cannot be null");
    }

    private PluginCommand getOrCreateCommand(String name) {
        try {
            Field commandMapField = Bukkit.getServer().getClass().getDeclaredField("commandMap");
            commandMapField.setAccessible(true);
            CommandMap commandMap = (CommandMap) commandMapField.get(Bukkit.getServer());

            Command existing = commandMap.getCommand(name.toLowerCase(Locale.ROOT));
            if (existing instanceof PluginCommand && ((PluginCommand) existing).getPlugin() == this.plugin) {
                 // If this plugin already registered it, return the existing command
                 if (((PluginCommand) existing).getExecutor() == this) {
                     return (PluginCommand) existing;
                 }
            }
            
            Constructor<PluginCommand> constructor = PluginCommand.class.getDeclaredConstructor(String.class, Plugin.class);
            constructor.setAccessible(true);
            PluginCommand newCmd = constructor.newInstance(name, plugin);
            
            commandMap.register(plugin.getName().toLowerCase(Locale.ROOT), newCmd);
            return newCmd;

        } catch (Exception e) {
            throw new RuntimeException("Failed to register or retrieve command '" + name + "'", e);
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
            e.printStackTrace(); // Log unexpected errors for debugging
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
