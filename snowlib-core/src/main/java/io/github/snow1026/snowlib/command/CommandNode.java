package io.github.snow1026.snowlib.command;

import org.bukkit.command.CommandSender;

import java.lang.reflect.Array;
import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.Function;

/**
 * Represents a node in the command tree.
 * <p>
 * Each node corresponds to an argument or sub-command,
 * and may have children, permissions, usage, description,
 * tab suggestions, or executors.
 * </p>
 */
public class CommandNode {

    private final Class<?> type;
    private final ArgumentParser<?> parser;
    private final List<CommandNode> children = new ArrayList<>();
    private BiConsumer<CommandContext, Object[]> executor;

    private String permission;
    private String description;
    private String usage;
    private List<String> aliases = Collections.emptyList();
    private Function<CommandSender, List<String>> suggester;

    /**
     * Creates a command node for a specific argument type.
     *
     * @param type Argument type.
     */
    public CommandNode(Class<?> type) {
        this.type = type;
        this.parser = ArgumentParsers.get(type);
    }

    /**
     * Adds a required argument.
     *
     * @param type    Argument type.
     * @param builder Builder to configure child node.
     * @param <T>     Type of argument.
     * @return Current node (for chaining).
     */
    public <T> CommandNode arg(Class<T> type, java.util.function.Consumer<CommandNode> builder) {
        CommandNode child = new CommandNode(type);
        builder.accept(child);
        children.add(child);
        return this;
    }

    /**
     * Adds an optional argument.
     *
     * @param type    Argument type.
     * @param builder Builder to configure child node.
     * @param <T>     Type of argument.
     * @return Current node (for chaining).
     */
    public <T> CommandNode optionalArg(Class<T> type, java.util.function.Consumer<CommandNode> builder) {
        return arg(type, builder);
    }

    /**
     * Adds a vararg (array) argument.
     *
     * @param arrayType Array class type.
     * @param action    Executor when command is run.
     * @return Current node (for chaining).
     */
    public CommandNode varArg(Class<?> arrayType, BiConsumer<CommandContext, Object[]> action) {
        if (!arrayType.isArray()) throw new IllegalArgumentException("VarArg requires an array type");
        this.executor = action;
        return this;
    }

    /**
     * Marks this node as executable with an action.
     *
     * @param action Executor.
     * @return Current node (for chaining).
     */
    public CommandNode exec(BiConsumer<CommandContext, Object[]> action) {
        this.executor = action;
        return this;
    }

    /**
     * Adds a literal fixed argument (like a subcommand).
     * <p>
     * Example:
     * <pre>
     * root.literal("test", child ->
     *     child.exec((ctx, args) -> {
     *         ctx.sender().sendMessage("You ran /sample test");
     *     })
     * );
     * </pre>
     *
     * @param value   Expected literal string.
     * @param builder Builder to configure child node.
     * @return Current node (for chaining).
     */
    public CommandNode literal(String value, java.util.function.Consumer<CommandNode> builder) {
        CommandNode child = new CommandNode(String.class) {
            @Override
            public boolean execute(CommandSender sender, String label, String[] args, int index, List<Object> collected) {
                if (index >= args.length) return false;
                if (!args[index].equalsIgnoreCase(value)) return false;

                // add the matched literal into collected
                List<Object> newCollected = new ArrayList<>(collected);
                newCollected.add(value);

                for (CommandNode c : children) {
                    if (c.execute(sender, label, args, index + 1, newCollected)) return true;
                }

                if (executor != null) {
                    executor.accept(new SimpleContext(sender, label, args, newCollected), newCollected.toArray());
                    return true;
                }
                return false;
            }

            @Override
            public List<String> suggest(CommandSender sender, String[] args, int index) {
                if (index >= args.length) return Collections.emptyList();
                if (args[index].isEmpty() || value.toLowerCase(Locale.ROOT).startsWith(args[index].toLowerCase(Locale.ROOT))) {
                    return List.of(value);
                }
                return Collections.emptyList();
            }
        };
        builder.accept(child);
        children.add(child);
        return this;
    }

    public CommandNode requires(String perm) { this.permission = perm; return this; }
    public CommandNode description(String desc) { this.description = desc; return this; }
    public CommandNode usage(String u) { this.usage = u; return this; }
    public CommandNode aliases(String... a) { this.aliases = Arrays.asList(a); return this; }
    public void suggests(Function<CommandSender, List<String>> s) { this.suggester = s;
    }

    /**
     * Executes this command node.
     */
    public boolean execute(CommandSender sender, String label, String[] args, int index, List<Object> collected) {
        if (permission != null && !sender.hasPermission(permission)) {
            sender.sendMessage("§cYou don't have permission: " + permission);
            return true;
        }

        if (type != null && type.isArray()) {
            Class<?> comp = type.getComponentType();
            ArgumentParser<?> compParser = ArgumentParsers.get(comp);
            int remain = Math.max(0, args.length - index);
            Object arr = Array.newInstance(comp, remain);
            for (int i = 0; i < remain; i++) {
                Object parsed = compParser.parse(sender, args[index + i]);
                if (parsed == null) return false;
                Array.set(arr, i, parsed);
            }
            List<Object> newCollected = new ArrayList<>(collected);
            newCollected.add(arr);
            for (CommandNode child : children) if (child.execute(sender, label, args, args.length, newCollected)) return true;
            if (executor != null) {
                executor.accept(new SimpleContext(sender, label, args, newCollected), newCollected.toArray());
                return true;
            }
            return false;
        }

        if (index >= args.length) {
            if (executor != null) {
                executor.accept(new SimpleContext(sender, label, args, collected), collected.toArray());
                return true;
            }
            return false;
        }

        Object parsed = (parser == null) ? args[index] : parser.parse(sender, args[index]);
        if (parsed == null) return false;

        List<Object> newCollected = new ArrayList<>(collected);
        newCollected.add(parsed);

        for (CommandNode child : children) if (child.execute(sender, label, args, index + 1, newCollected)) return true;

        if (executor != null) {
            executor.accept(new SimpleContext(sender, label, args, newCollected), newCollected.toArray());
            return true;
        }

        return false;
    }

    /**
     * Generates tab completion suggestions.
     */
    public List<String> suggest(CommandSender sender, String[] args, int index) {
        if (index >= args.length) return Collections.emptyList();
        List<String> out = new ArrayList<>();
        if (suggester != null) out.addAll(suggester.apply(sender));
        else if (type != null && type.isEnum()) {
            for (Object val : type.getEnumConstants()) out.add(val.toString());
        } else if (type != null && type.isArray()) {
            Class<?> comp = type.getComponentType();
            out.addAll(ArgumentParsers.get(comp).suggest(sender, args[index]));
        } else if (parser != null) out.addAll(parser.suggest(sender, args[index]));
        for (CommandNode child : children) out.addAll(child.suggest(sender, args, index + 1));
        return out;
    }

    public String getDescription() { return description; }
    public String getUsage() { return usage; }
    public List<String> getAliases() { return aliases; }
}
