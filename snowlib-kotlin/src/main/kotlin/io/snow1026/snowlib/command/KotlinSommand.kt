package io.snow1026.snowlib.command

import kotlin.reflect.KClass

/**
 * Kotlin-friendly wrapper for SnowLib Sommand.
 * Provides DSL-style registration with lambdas and type-safe arguments.
 *
 * Example:
 * ```
 * val commands = KotlinSommand(plugin)
 *
 * commands.register("send") {
 *     arg(Player::class) {
 *         varArg(Array<String>::class) { ctx, args ->
 *             val target = ctx.arg<Player>(0)
 *             val msg = ctx.arg<Array<String>>(1).joinToString(" ")
 *             target.sendMessage("§a[MSG] $msg")
 *         }
 *     }
 * }
 * ```
 */
class KotlinSommand(plugin: org.bukkit.plugin.java.JavaPlugin) {

    private val javaCommand = Sommand(plugin)

    /**
     * Registers a command using Kotlin DSL.
     *
     * @param name Command name.
     * @param block DSL block to configure the [CommandNode].
     */
    fun register(name: String, block: CommandNode.() -> Unit) {
        javaCommand.register(name) { it.block() }
    }

    /**
     * Access underlying Java Sommand instance.
     */
    fun java(): Sommand = javaCommand
}

/**
 * Kotlin DSL helper: register an argument node with [KClass].
 */
fun <T : Any> CommandNode.arg(type: KClass<T>, block: CommandNode.() -> Unit) {
    this.arg(type.java, block)
}

/**
 * Kotlin DSL helper: register a vararg node with [KClass].
 */
fun <T : Any> CommandNode.varArg(type: KClass<Array<T>>, block: (ctx: CommandContext, args: Array<Any>) -> Unit) {
    this.varArg(type.java, block)
}

/**
 * Kotlin DSL helper: mark a node executable.
 */
fun CommandNode.exec(block: (ctx: CommandContext, args: Array<Any>) -> Unit) {
    this.exec(block)
}

/**
 * Kotlin DSL helper: add a literal subcommand.
 */
fun CommandNode.literal(value: String, block: (ctx: CommandContext, args: Array<Any>) -> Unit) {
    this.literal(value, block)
}
