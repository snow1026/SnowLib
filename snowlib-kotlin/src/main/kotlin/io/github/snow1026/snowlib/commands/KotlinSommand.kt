package io.github.snow1026.snowlib.commands

import org.bukkit.plugin.java.JavaPlugin
import kotlin.reflect.KClass

/**
 * A Kotlin-friendly DSL wrapper for the SnowLib Sommand framework.
 * This class provides a more idiomatic way to define commands in Kotlin.
 *
 * Example:
 * ```kotlin
 * val commands = KotlinSommand(this)
 *
 * commands.register("team") {
 * description("Manage teams")
 * alias("t")
 * usage("/<command> <create|list> [args]")
 *
 * sub("create") {
 * sub("name", String::class) {
 * executes { ctx ->
 * val teamName = ctx.getArgument<String>("name")
 * ctx.sender.sendMessage("§aTeam '$teamName' created!")
 * }
 * }
 * }
 * sub("list") {
 * executes { ctx ->
 * ctx.sender.sendMessage("§eTeams: Alpha, Beta, Gamma")
 * }
 * }
 * }
 * ```
 */
class KotlinSommand(plugin: JavaPlugin) {

    private val javaSommand = Sommand(plugin)

    /**
     * Registers a new command using the Kotlin DSL.
     *
     * @param name The name of the command.
     * @param block The DSL block for configuring the [SommandNode].
     */
    fun register(name: String, block: SommandNode.() -> Unit) {
        javaSommand.register(name).apply(block)
    }

    /**
     * Provides access to the underlying Java {@link Sommand} instance for advanced configuration.
     * @return The raw Java Sommand instance.
     */
    fun java(): Sommand = javaSommand
}

// --- DSL Helpers ---

/**
 * DSL helper to define a required argument sub-command.
 *
 * @param name The name of the argument.
 * @param type The KClass of the argument's type.
 * @param block A block to configure the argument's node.
 */
fun <T : Any> SommandNode.sub(name: String, type: KClass<T>, block: SommandNode.() -> Unit = {}) {
    this.sub(name, type.java, block)
}

/**
 * DSL helper to define a literal sub-command.
 *
 * @param name The name of the literal.
 * @param block A block to configure the literal's node.
 */
fun SommandNode.sub(name: String, block: SommandNode.() -> Unit = {}) {
    this.literal(name, block)
}

/**
 * DSL helper to define the execution logic for a command node.
 *
 * @param block The action to perform on execution.
 */
fun SommandNode.executes(block: (ctx: SommandContext) -> Unit) {
    this.executes(block)
}

/**
 * DSL helper to set aliases for the command. Only effective on the root node.
 * @param aliases Vararg of alias strings.
 */
fun SommandNode.alias(vararg aliases: String) {
    this.alias(*aliases)
}

/**
 * DSL helper to set the description for the command. Only effective on the root node.
 * @param description The command description.
 */
fun SommandNode.description(description: String) {
    this.description(description)
}

/**
 * DSL helper to set the usage message for the command. Only effective on the root node.
 * @param usage The usage message.
 */
fun SommandNode.usage(usage: String) {
    this.usage(usage)
}


/**
 * A convenience extension to get a non-nullable argument from the context.
 *
 * @param T The expected type of the argument.
 * @param name The name of the argument.
 * @return The parsed argument.
 * @throws IllegalArgumentException if the argument is not found.
 */
inline fun <reified T> SommandContext.getArgument(name: String): T {
    return this.getArgument(name)
}
