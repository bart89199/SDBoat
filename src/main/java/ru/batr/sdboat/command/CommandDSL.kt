package ru.batr.sdboat.command

import TextFormatter
import net.kyori.adventure.audience.Audience
import net.kyori.adventure.text.Component
import org.bukkit.command.Command
import org.bukkit.command.CommandSender
import org.bukkit.command.PluginCommand
import org.bukkit.command.TabExecutor
import ru.batr.sdboat.SDBoat
import ru.batr.sdboat.SDBoat.Companion.adventure
import ru.batr.sdboat.SDBoat.Companion.sendMessage
import kotlin.reflect.KProperty

@DslMarker
annotation class CommandMarker

/**
 * Represent onTabComplete and onCommand data with [curArgs] that represents current arguments
 */
data class CommandData(
    val sender: CommandSender,
    val command: Command,
    val label: String,
    val curArgs: List<String>,
    val args: List<String>
)

/**
 * Represent command feedback action
 */
@CommandMarker
fun interface Action {
    /**
     * @param commandData current data
     * @return exit status
     */
    fun execute(commandData: CommandData): Boolean
}
//TODO rewrite
fun interface TabAction : Action

/**
 * Modifier for [Action] that is displayed in advices
 */
fun interface TabHolder {
    /**
     * @param commandData current data
     * @return List of advices and exit status (if true call chain ends)
     */
    fun onTabComplete(commandData: CommandData): Pair<MutableList<String>, Boolean>
}

interface ActionsHolder {
    val actions: MutableList<Action>
}

/**
 * Command argument
 * @param ignoringInnerExit if true, ignore inner exit and continue executing, by default false
 */
abstract class Argument(var ignoringInnerExit: Boolean = false) : Action, ActionsHolder {
    override val actions = ArrayList<Action>()
    override fun execute(commandData: CommandData): Boolean {
        val newData: CommandData
        return if (checkArg(commandData).also { newData = it.second }.first) {
            actions.forEach {
                if (it !is TabAction)
                    if (it.execute(newData) && !ignoringInnerExit) return true
            }
            false
        } else false
    }

    /**
     * @param commandData current data
     * @return check status, new data
     */
    abstract fun checkArg(commandData: CommandData): Pair<Boolean, CommandData>
}

/**
 * Variation of [Argument] with [TabHolder]
 */
abstract class TabArgument(ignoringInnerExit: Boolean = false) :
    Argument(ignoringInnerExit), TabHolder {
    override fun onTabComplete(commandData: CommandData): Pair<MutableList<String>, Boolean> {
        if (commandData.curArgs.size == 1) return tabList(commandData) to false
        val newData = checkArg(commandData).also { if (!it.first) return tabList(commandData) to false }.second
        val list = ArrayList<String>()
        for (action in actions) {
            if (action is TabAction) {
                if (action.execute(commandData)) return mutableListOf("Где-то ошибка!") to true
            }
            if (action is TabHolder) {
                list.addAll(action.onTabComplete(newData).also { if (it.second) return it }.first)
            }
        }
        return list to true
    }

    /**
     * advice list generator
     */
    abstract fun tabList(commandData: CommandData): MutableList<String>
}

/**
 * Fixed command argument
 * @param argNames argument name and alias (' ' not allowed)
 * @param ignoreCase ignores case in comparing
 * @param onTabList advice list
 * @param sortAdvicesWithTyped only advices that start with typed will be shown
 */
open class FixedArgument(
    val argNames: MutableList<String>,
    var ignoreCase: Boolean = true,
    val onTabList: MutableList<String> = argNames,
    var sortAdvicesWithTyped: Boolean = true,
    ignoreInnerExit: Boolean = false,
) : TabArgument(ignoreInnerExit) {
    init {
        if (ignoreCase) {
            repeat(argNames.size) { i ->
                argNames[i] = argNames[i].lowercase()
            }
        }
    }

    override fun tabList(commandData: CommandData): MutableList<String> =
        if (sortAdvicesWithTyped && commandData.curArgs.isNotEmpty()) onTabList.filter { it.startsWith(commandData.curArgs[0]) }
            .toMutableList() else onTabList

    override fun checkArg(commandData: CommandData): Pair<Boolean, CommandData> {
        if (commandData.curArgs.isEmpty()) return false to commandData
        return argNames.contains(if (ignoreCase) commandData.curArgs[0].lowercase() else commandData.curArgs[0]) to commandData.copy(
            curArgs = commandData.curArgs.drop(1)
        )
    }
}

/**
 * Input reader
 * @param onExceptionAction action that will be executed if converting will crash
 */
abstract class AbstractInputArgument<T>(
    var openSeparator: String = " ",
    var closeSeparator: String = openSeparator,
    ignoringInnerExit: Boolean = false,
    var onExceptionAction: Action? = null,
) : TabArgument(ignoringInnerExit) {
    protected var _value: T? = null

    @Suppress("UNCHECKED_CAST")
    operator fun getValue(thisRef: Any?, property: KProperty<*>): T = _value as T

    override fun checkArg(commandData: CommandData): Pair<Boolean, CommandData> = try {
        true to convert(commandData).also { _value = it.first }.second
    } catch (e: IllegalCommandArgumentException) {
        false to commandData
    }

    override fun execute(commandData: CommandData): Boolean {
        return try {
            val newData: CommandData = convert(commandData).also { _value = it.first }.second
            actions.forEach {
                if (it !is TabAction)
                    if (it.execute(newData) && !ignoringInnerExit) return true
            }
            false
        } catch (e: IllegalCommandArgumentException) {
            onExceptionAction?.execute(commandData)
            false
        }
    }

    /**
     * convert [CommandData] to [T]
     */
    fun convert(commandData: CommandData): Pair<T, CommandData> {
        val newArgs = commandData.curArgs.toMutableList()
        if (newArgs.isEmpty()) throw IllegalCommandArgumentException()
        if (openSeparator == " ") {
            if (closeSeparator == " ") return convert(newArgs[0]) to commandData.copy(curArgs = newArgs.drop(1))
        } else {
            if (!newArgs[0].startsWith(openSeparator)) throw IllegalCommandArgumentException()
            newArgs[0] = newArgs[0].removePrefix(openSeparator)
        }
        val builder = StringBuilder()
        newArgs.forEachIndexed { i, str ->
            if (str.endsWith(closeSeparator)) {
                builder.append(str.removeSuffix(closeSeparator))
                return convert(builder.toString()) to commandData.copy(
                    curArgs = newArgs.subList(i + 1, newArgs.size)
                )
            } else builder.append("$str ")
        }
        throw IllegalCommandArgumentException()
    }

    /**
     * Convert [String] to [T]
     */
    abstract fun convert(input: String): T
}

class InputArgument<T>(
    openSeparator: String = " ",
    closeSeparator: String = openSeparator,
    ignoringInnerExit: Boolean = false,
    onExceptionAction: Action? = null,
    var convertor: (String) -> T,
    var onTab: CommandData.() -> MutableList<String>
) : AbstractInputArgument<T>(openSeparator, closeSeparator, ignoringInnerExit, onExceptionAction) {
    override fun convert(input: String): T = convertor(input)

    override fun tabList(commandData: CommandData): MutableList<String> = onTab(commandData)
}

abstract class AbstractInputListArgument<T>(
    openSeparator: String = " ",
    closeSeparator: String = openSeparator,
    var separators: MutableList<String> = mutableListOf(", ", ","),
    var argumentRange: IntRange? = null,
    ignoringInnerExit: Boolean = false,
    onExceptionAction: Action? = null,
    var removeArgAdviceIfUsed: Boolean = false,
    var sortAdvicesWithTyped: Boolean = true,
) : AbstractInputArgument<List<T>>(openSeparator, closeSeparator, ignoringInnerExit, onExceptionAction) {

    override fun convert(input: String): List<T> =
        input.split(*separators.toTypedArray()).map {
            convertElement(it)
        }.also { list -> argumentRange?.let { if (list.size !in it) throw IllegalCommandArgumentException() } }

    override fun tabList(commandData: CommandData): MutableList<String> {
        try {
            convert(commandData)
            return ArrayList()
        } catch (e: IllegalCommandArgumentException) {
            val newArgs = commandData.curArgs.toMutableList()
            if (newArgs.isEmpty()) return tabListContent(commandData)
            if (openSeparator == " ") {
                if (closeSeparator == " ")
                    return tabListContent(commandData, commandData.curArgs[0]).filter { it.startsWith(commandData.curArgs[0]) }.toMutableList()
            } else {
                if (!newArgs[0].startsWith(openSeparator)) return tabListContent(commandData).map { openSeparator + it }
                    .toMutableList().also { if (it.isEmpty()) return mutableListOf(openSeparator) }
                newArgs[0] = newArgs[0].removePrefix(openSeparator)
            }
            val builder = StringBuilder()
            newArgs.forEach { str ->
                if (str.endsWith(closeSeparator)) {
                    builder.append(str.removeSuffix(closeSeparator))
                } else {
                    if (builder.isNotBlank()) builder.append(" ")
                    builder.append(str)
                }
            }
            val args = builder.split(*separators.toTypedArray()).toMutableList()
            args.forEachIndexed { i, str ->
                if (i + 1 != args.size) try {
                    convertElement(str)
                } catch (e: IllegalCommandArgumentException) {
                    return mutableListOf("Введён неверный тип!")
                }
            }
            if (args.isEmpty()) return tabListContent(commandData).map { commandData.args.last() + it }.toMutableList()
            val advices = tabListContent(commandData, args.last()).filter {
                if (removeArgAdviceIfUsed && args.contains(it)) {
                    args.remove(it)
                    false
                } else !sortAdvicesWithTyped || it.startsWith(args.last())
            }.toMutableList()
            val range = argumentRange
            if (range == null || args.size in range || args.size < range.first) {
                if (args.last().isNotBlank()) {
                    if (range == null || args.size != range.last) advices.add(separators.first())
                    if (range == null || args.size >= range.first) advices.add(closeSeparator)
                }
                return advices.map {

                    commandData.args.last() + if (it.startsWith(args.last())) it.removePrefix(args.last()) else it
                }.toMutableList()
            } else return mutableListOf("Введённо слишком много аргументов!")
        }
    }

    abstract fun tabListContent(commandData: CommandData, lastArg: String? = null): MutableList<String>
    abstract fun convertElement(string: String): T
}

class InputListArgument<T>(
    openSeparator: String = " ",
    closeSeparator: String = openSeparator,
    separators: MutableList<String> = mutableListOf(", ", ","),
    argumentRange: IntRange? = null,
    ignoringInnerExit: Boolean = false,
    onExceptionAction: Action? = null,
    removeArgAdviceIfUsed: Boolean = false,
    sortAdvicesWithTyped: Boolean = true,
    var convertor: (String) -> T,
    var onTab: CommandData.(lastArg: String?) -> MutableList<String>,
) : AbstractInputListArgument<T>(
    openSeparator,
    closeSeparator,
    separators,
    argumentRange,
    ignoringInnerExit,
    onExceptionAction,
    removeArgAdviceIfUsed,
    sortAdvicesWithTyped
) {
    override fun convertElement(string: String): T = convertor(string)
    override fun tabListContent(commandData: CommandData, lastArg: String?): MutableList<String> = onTab(commandData, lastArg)
}


class IllegalCommandArgumentException(
    val componentMessage: Component? = null
) : Exception()

/**
 * Command class
 * @param name command name
 */
@CommandMarker
class SDCommand(val name: String) : TabExecutor, ActionsHolder {

    private val bukkitCommand: PluginCommand =
        SDBoat.instance.getCommand(name) ?: throw IllegalArgumentException("Can't found such command '\'$name\'")
    override val actions = ArrayList<Action>()

    init {
        bukkitCommand.setExecutor(this)
        bukkitCommand.tabCompleter = this
    }


    override fun onTabComplete(
        sender: CommandSender,
        command: Command,
        label: String,
        args: Array<out String>?
    ): MutableList<String> {
        val argsList = args?.toList() ?: emptyList()
        val commandData = CommandData(sender, command, label, argsList, argsList)
        val list = ArrayList<String>()
        for (action in actions) {
            if (action is TabAction) {
                if (action.execute(commandData)) return mutableListOf("Где-то ошибка!")
            }
            if (action is TabHolder) {
                list.addAll(action.onTabComplete(commandData).also { if (it.second) return it.first }.first)
            }
        }
        return list
    }

    override fun onCommand(
        sender: CommandSender,
        command: Command,
        label: String,
        args: Array<out String>?
    ): Boolean {
        val argsList = args?.toList() ?: emptyList()
        for (action in actions) {
            if (action is TabAction) continue
            if (action.execute(CommandData(sender, command, label, argsList, argsList))) break
        }
        return true
    }
}

fun command(name: String, init: SDCommand.() -> Unit) = SDCommand(name).apply(init)

fun ActionsHolder.action(exit: Boolean = false, action: CommandData.() -> Unit) {
    actions.add { action(it); exit }
}

fun ActionsHolder.action1(action: CommandData.() -> Boolean) {
    actions.add(action)
}

fun ActionsHolder.lastAction(action: CommandData.() -> Unit) {
    actions.add { action(it); true }
}

fun ActionsHolder.tabAction(exit: Boolean = false, action: CommandData.() -> Unit) {
    actions.add(TabAction { action(it); exit })
}

fun ActionsHolder.tabAction1(action: CommandData.() -> Boolean) {
    actions.add(TabAction(action))
}

fun ActionsHolder.lastTabAction(action: CommandData.() -> Unit) {
    actions.add(TabAction { action(it); true })
}

fun ActionsHolder.exit() {
    actions.add { true }
}

fun ActionsHolder.arg(
    vararg name: String,
    onTabList: MutableList<String> = name.toMutableList(),
    ignoreCase: Boolean = true,
    ignoreInnerExit: Boolean = false,
    sortAdvicesWithTyped: Boolean = true,
    init: FixedArgument.() -> Unit
) {
    val argument = FixedArgument(name.toMutableList(), ignoreCase, onTabList, sortAdvicesWithTyped, ignoreInnerExit)
    argument.init()
    actions.add(argument)
}

// TODO Add exception handle support in onExceptionAction
// TODO Advice as list order

val toString = { input: String ->
    if (input.isBlank()) throw IllegalCommandArgumentException()
    input
}
val toComponent = { input: String ->
    TextFormatter.format(input)
}
val toInt = { input: String ->
    try {
        input.toInt()
    } catch (e: NumberFormatException) {
        throw IllegalCommandArgumentException()
    }
}
val toDouble = { input: String ->
    try {
        input.toDouble()
    } catch (e: NumberFormatException) {
        throw IllegalCommandArgumentException()
    }
}
val toBoolean = convertor@{ input: String ->
    input.replace("true", "1").replace("да", "0").replace("false", "0").replace("нет", "0")
        .also { if (it == "0") return@convertor false }.also { if (it == "1") return@convertor true }
    throw IllegalCommandArgumentException()
}

operator fun String.unaryPlus(): CommandData.() -> MutableList<String> = { mutableListOf(this@unaryPlus) }
operator fun String.unaryMinus(): CommandData.(lastArg: String?) -> MutableList<String> = { mutableListOf(this@unaryMinus) }
operator fun Iterable<String>.unaryPlus(): CommandData.() -> MutableList<String> = { this@unaryPlus.toMutableList() }
operator fun Iterable<String>.unaryMinus(): CommandData.(lastArg: String?) -> MutableList<String> = { this@unaryMinus.toMutableList() }

fun <T> ActionsHolder.input(
    onTab: CommandData.() -> MutableList<String> = +"[ввод]",
    openSeparator: String = " ",
    closeSeparator: String = openSeparator,
    onExceptionAction: Action? = null,
    ignoringInnerExit: Boolean = false,
    convertor: (String) -> T,
    init: InputArgument<T>.() -> Unit
) {
    val argument = InputArgument(openSeparator, closeSeparator, ignoringInnerExit, onExceptionAction, convertor, onTab)
    argument.init()
    actions.add(argument)
}

fun ActionsHolder.inputString(
    onTab: CommandData.() -> MutableList<String> = +"ввод",
    openSeparator: String = " ",
    closeSeparator: String = openSeparator,
    onExceptionAction: Action? = null,
    ignoringInnerExit: Boolean = false,
    init: InputArgument<String>.() -> Unit
) {
    input(
        onTab = onTab,
        openSeparator = openSeparator,
        closeSeparator = closeSeparator,
        onExceptionAction = onExceptionAction,
        ignoringInnerExit = ignoringInnerExit,
        convertor = toString,
        init = init
    )
}

fun ActionsHolder.inputComponent(
    onTab: CommandData.() -> MutableList<String> = +"ввод",
    openSeparator: String = " ",
    closeSeparator: String = openSeparator,
    onExceptionAction: Action? = null,
    ignoringInnerExit: Boolean = false,
    init: InputArgument<Component>.() -> Unit
) {
    input(
        onTab = onTab,
        openSeparator = openSeparator,
        closeSeparator = closeSeparator,
        onExceptionAction = onExceptionAction,
        ignoringInnerExit = ignoringInnerExit,
        convertor = toComponent,
        init = init
    )
}

fun ActionsHolder.inputInt(
    onTab: CommandData.() -> MutableList<String> = +"число",
    openSeparator: String = " ",
    closeSeparator: String = openSeparator,
    onExceptionAction: Action? = null,
    ignoringInnerExit: Boolean = false,
    init: InputArgument<Int>.() -> Unit
) {
    input(
        onTab = onTab,
        openSeparator = openSeparator,
        closeSeparator = closeSeparator,
        onExceptionAction = onExceptionAction,
        ignoringInnerExit = ignoringInnerExit,
        convertor = toInt,
        init = init
    )
}

fun ActionsHolder.inputDouble(
    onTab: CommandData.() -> MutableList<String> = +"число",
    openSeparator: String = " ",
    closeSeparator: String = openSeparator,
    onExceptionAction: Action? = null,
    ignoringInnerExit: Boolean = false,
    init: InputArgument<Double>.() -> Unit
) {
    input(
        onTab = onTab,
        openSeparator = openSeparator,
        closeSeparator = closeSeparator,
        onExceptionAction = onExceptionAction,
        ignoringInnerExit = ignoringInnerExit,
        convertor = toDouble,
        init = init
    )
}

fun ActionsHolder.inputBoolean(
    onTab: CommandData.() -> MutableList<String> = +listOf("да", "нет"),
    openSeparator: String = " ",
    closeSeparator: String = openSeparator,
    onExceptionAction: Action? = null,
    ignoringInnerExit: Boolean = false,
    init: InputArgument<Boolean>.() -> Unit
) {
    input(
        onTab = onTab,
        openSeparator = openSeparator,
        closeSeparator = closeSeparator,
        onExceptionAction = onExceptionAction,
        ignoringInnerExit = ignoringInnerExit,
        convertor = toBoolean,
        init = init
    )
}

fun <T> ActionsHolder.inputList(
    onTab: CommandData.(lastArg: String?) -> MutableList<String> = -"список",
    argumentRange: IntRange? = null,
    openSeparator: String = "[",
    closeSeparator: String = "]",
    separators: MutableList<String> = mutableListOf(", ", ","),
    onExceptionAction: Action? = null,
    ignoringInnerExit: Boolean = false,
    removeArgAdviceIfUsed: Boolean = false,
    sortAdvicesWithTyped: Boolean = true,
    convertor: (String) -> T,
    init: InputListArgument<T>.() -> Unit
) {
    val argument = InputListArgument(
        openSeparator,
        closeSeparator,
        separators,
        argumentRange,
        ignoringInnerExit,
        onExceptionAction,
        removeArgAdviceIfUsed,
        sortAdvicesWithTyped,
        convertor,
        onTab
    )
    argument.init()
    actions.add(argument)
}

fun ActionsHolder.inputStringList(
    onTab: CommandData.(lastArg: String?) -> MutableList<String> = -"имя",
    argumentRange: IntRange? = null,
    openSeparator: String = "[",
    closeSeparator: String = "]",
    separators: MutableList<String> = mutableListOf(", ", ","),
    onExceptionAction: Action? = null,
    ignoringInnerExit: Boolean = false,
    removeArgAdviceIfUsed: Boolean = false,
    sortAdvicesWithTyped: Boolean = true,
    init: InputListArgument<String>.() -> Unit
) {
    inputList(
        onTab = onTab,
        openSeparator = openSeparator,
        closeSeparator = closeSeparator,
        separators = separators,
        argumentRange = argumentRange,
        onExceptionAction = onExceptionAction,
        ignoringInnerExit = ignoringInnerExit,
        removeArgAdviceIfUsed = removeArgAdviceIfUsed,
        sortAdvicesWithTyped = sortAdvicesWithTyped,
        convertor = toString,
        init = init
    )
}


fun ActionsHolder.inputComponentList(
    onTab: CommandData.(lastArg: String?) -> MutableList<String> = -"имя",
    argumentRange: IntRange? = null,
    openSeparator: String = "[",
    closeSeparator: String = "]",
    separators: MutableList<String> = mutableListOf(", ", ","),
    onExceptionAction: Action? = null,
    ignoringInnerExit: Boolean = false,
    removeArgAdviceIfUsed: Boolean = false,
    sortAdvicesWithTyped: Boolean = true,
    init: InputListArgument<Component>.() -> Unit
) {
    inputList(
        onTab = onTab,
        openSeparator = openSeparator,
        closeSeparator = closeSeparator,
        separators = separators,
        argumentRange = argumentRange,
        onExceptionAction = onExceptionAction,
        ignoringInnerExit = ignoringInnerExit,
        removeArgAdviceIfUsed = removeArgAdviceIfUsed,
        sortAdvicesWithTyped = sortAdvicesWithTyped,
        convertor = toComponent,
        init = init
    )
}

fun ActionsHolder.inputIntList(
    onTab: CommandData.(lastArg: String?) -> MutableList<String> = -"число",
    argumentRange: IntRange? = null,
    openSeparator: String = "[",
    closeSeparator: String = "]",
    separators: MutableList<String> = mutableListOf(", ", ","),
    onExceptionAction: Action? = null,
    ignoringInnerExit: Boolean = false,
    removeArgAdviceIfUsed: Boolean = false,
    sortAdvicesWithTyped: Boolean = true,
    init: InputListArgument<Int>.() -> Unit
) {
    inputList(
        onTab = onTab,
        openSeparator = openSeparator,
        closeSeparator = closeSeparator,
        separators = separators,
        argumentRange = argumentRange,
        onExceptionAction = onExceptionAction,
        ignoringInnerExit = ignoringInnerExit,
        removeArgAdviceIfUsed = removeArgAdviceIfUsed,
        sortAdvicesWithTyped = sortAdvicesWithTyped,
        convertor = toInt,
        init = init
    )
}

fun ActionsHolder.inputDoubleList(
    onTab: CommandData.(lastArg: String?) -> MutableList<String> = -"число",
    argumentRange: IntRange? = null,
    openSeparator: String = "[",
    closeSeparator: String = "]",
    separators: MutableList<String> = mutableListOf(", ", ","),
    onExceptionAction: Action? = null,
    ignoringInnerExit: Boolean = false,
    removeArgAdviceIfUsed: Boolean = false,
    sortAdvicesWithTyped: Boolean = true,
    init: InputListArgument<Double>.() -> Unit
) {
    inputList(
        onTab = onTab,
        openSeparator = openSeparator,
        closeSeparator = closeSeparator,
        separators = separators,
        argumentRange = argumentRange,
        onExceptionAction = onExceptionAction,
        ignoringInnerExit = ignoringInnerExit,
        removeArgAdviceIfUsed = removeArgAdviceIfUsed,
        sortAdvicesWithTyped = sortAdvicesWithTyped,
        convertor = toDouble,
        init = init
    )
}

fun ActionsHolder.inputBooleanList(
    onTab: CommandData.(lastArg: String?) -> MutableList<String> = -listOf("да", "нет"),
    argumentRange: IntRange? = null,
    openSeparator: String = "[",
    closeSeparator: String = "]",
    separators: MutableList<String> = mutableListOf(", ", ","),
    onExceptionAction: Action? = null,
    ignoringInnerExit: Boolean = false,
    removeArgAdviceIfUsed: Boolean = false,
    sortAdvicesWithTyped: Boolean = true,
    init: InputListArgument<Boolean>.() -> Unit
) {
    inputList(
        onTab = onTab,
        openSeparator = openSeparator,
        closeSeparator = closeSeparator,
        separators = separators,
        argumentRange = argumentRange,
        onExceptionAction = onExceptionAction,
        ignoringInnerExit = ignoringInnerExit,
        removeArgAdviceIfUsed = removeArgAdviceIfUsed,
        sortAdvicesWithTyped = sortAdvicesWithTyped,
        convertor = toBoolean,
        init = init
    )
}