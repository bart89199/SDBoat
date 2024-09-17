package ru.batr.sdboat.command

import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.entity.Player
import ru.batr.sdboat.SDBoat.Companion.adventure
import ru.batr.sdboat.SDBoat.Companion.mainConfig
import ru.batr.sdboat.SDBoat.Companion.sendMessagePr
import ru.batr.sdboat.database.Database
import ru.batr.sdboat.racemap.*

object ManageCommand {
    fun init() {
        command("boatracemanage") {
            arg("reload") {
                lastAction {
                    mainConfig.reload()
                    Database.availableRaceMaps.toMutableMap().forEach { (raceMap, _) -> raceMap.prepareStart() }
                    sender.sendMessagePr("<green>Конфиг успешно перезагружен</green>")
                }
            }
            arg("create", "создать") {
                inputString(+"имяКарты") {
                    val name by this
                    world {
                        val world by this
                        action {
                            if (Bukkit.getWorld(world) == null) sender.sendMessagePr("<red>Мир $world не найден!</red>")
                        }
                        twoCoordinates {
                            val start by this
                            twoCoordinates {
                                val end by this
                                twoCoordinates {
                                    val finishStart by this
                                    twoCoordinates {
                                        val finishEnd by this
                                        lastAction {
                                            if (
                                                Database.addRaceMap(
                                                    RaceMap(
                                                        name,
                                                        world,
                                                        SimpleCoordinates(start[0], start[1]),
                                                        SimpleCoordinates(end[0], end[1]),
                                                        SimpleCoordinates(finishStart[0], finishStart[1]),
                                                        SimpleCoordinates(finishEnd[0], finishEnd[1])
                                                    )
                                                )
                                            ) {
                                                sender.sendMessagePr("<green>Карта успешно создана!</green>")
                                            } else {
                                                sender.sendMessagePr("<red>Такая карта уже существует!</red>")
                                            }
                                        }
                                    }
                                }
                            }
                        }

                    }
                }
                lastAction {
                    sender.sendMessagePr("<red>Ошибка!</red> <gold>Формат: $label ${args[0]} [имя карты] [мир] [координаты начала(x, z)] [координаты конца(x, z)] [координаты начала зоны финиша(x, z)] [координаты конца зоны финиша(x, z)]</gold>")
                }
            }
            arg("check", "проверить") {
                lastAction {
                    sender.sendMessagePr("Карты:")
                    Database.raceMaps.forEach { map ->
                        sender.sendMessagePr("<aqua>${map.name} <grey>=</grey> ${map.status.name}</aqua>")
                    }
                }
            }
            arg("delete", "удалить") {
                inputString {
                    onTab = { Database.raceMaps.map { it.name }.toMutableList() }
                    val mapName by this
                    lastAction {
                        if (Database.raceMaps.map { it.name }.contains(mapName)) {
                            Database.removeRaceMap(mapName)
                            sender.sendMessagePr("<green>Карта <aqua>$mapName</aqua> успешно удалена!</green>")
                        } else {
                            sender.sendMessagePr("<red>Ошибка! Карта не найдена</red>")
                        }
                    }
                }
            }
            arg("select", "выбрать") {
                inputString {
                    onTab = { Database.raceMaps.map { it.name }.toMutableList() }
                    val mapName by this
                    lateinit var raceMap: RaceMap
                    action1 {
                        raceMap = Database.raceMaps.find { it.name == mapName }.let {
                            if (it == null) {
                                sender.sendMessagePr("<red>Ошибка! Карта не найдена</red>")
                                return@action1 true
                            } else it
                        }
                        false
                    }
                    tabAction1 {
                        raceMap = Database.raceMaps.find { it.name == mapName }.let {
                            it ?: return@tabAction1 true
                        }
                        false
                    }
                    arg("edit", "изменить") {
                        arg("name") {
                            inputString(+"новоеИмя") {
                                val newName by this
                                lastAction {
                                    if (Database.updateRaceMap(raceMap.name, raceMap.copy(name = newName))) {
                                        sender.sendMessagePr("<green>Имя успешно изменено!</green>")
                                    } else {
                                        sender.sendMessagePr("<red>Такая карта уже существует!</red>")
                                    }
                                }
                            }
                            lastAction {
                                sender.sendMessagePr("<red>Ошибка!</red> <gold>Формат: $label ${args[0]} ${args[1]} ${args[2]} ${args[3]} [новоеИмя]</gold>")
                            }
                        }
                        arg("world") {
                            world {
                                val world by this
                                lastAction {
                                    val w = Bukkit.getWorld(world)
                                    if (w == null) {
                                        sender.sendMessagePr("<red>Ошибка! Мир не найден!</red>")
                                    } else {
                                        raceMap.world = world
                                        raceMap.update()
                                        sender.sendMessagePr("<green>Мир успешно изменён!</green>")
                                    }
                                }
                            }
                        }
                        arg("start") {
                            var coordinates: SimpleCoordinates? = null
                            action {
                                coordinates = null
                                if (sender is Player) coordinates = -(-sender.location)
                            }
                            twoCoordinates {
                                val cords by this
                                action {
                                    coordinates = SimpleCoordinates(cords[0], cords[1])
                                }
                            }

                            lastAction {
                                val cords = coordinates
                                if (cords == null) {
                                    sender.sendMessagePr("<red>Ошибка!</red> <gold>Формат: $label ${args[0]} ${args[1]} ${args[2]} ${args[3]} [координаты(x, z)]</gold>")
                                } else {
                                    raceMap.start = cords
                                    raceMap.update()
                                    sender.sendMessagePr("<green>Локация успешно изменена!</green>")
                                }
                            }
                        }
                        arg("end") {
                            var coordinates: SimpleCoordinates? = null
                            action {
                                coordinates = null
                                if (sender is Player) coordinates = -(-sender.location)
                            }
                            twoCoordinates {
                                val cords by this
                                action {
                                    coordinates = SimpleCoordinates(cords[0], cords[1])
                                }
                            }

                            lastAction {
                                val cords = coordinates
                                if (cords == null) {
                                    sender.sendMessagePr("<red>Ошибка!</red> <gold>Формат: $label ${args[0]} ${args[1]} ${args[2]} ${args[3]} [координаты(x, z)]</gold>")
                                } else {
                                    raceMap.end = cords
                                    raceMap.update()
                                    sender.sendMessagePr("<green>Локация успешно изменена!</green>")
                                }
                            }
                        }
                        arg("finishStart") {
                            var coordinates: SimpleCoordinates? = null
                            action {
                                coordinates = null
                                if (sender is Player) coordinates = -(-sender.location)
                            }
                            twoCoordinates {
                                val cords by this
                                action {
                                    coordinates = SimpleCoordinates(cords[0], cords[1])
                                }
                            }

                            lastAction {
                                val cords = coordinates
                                if (cords == null) {
                                    sender.sendMessagePr("<red>Ошибка!</red> <gold>Формат: $label ${args[0]} ${args[1]} ${args[2]} ${args[3]} [координаты(x, z)]</gold>")
                                } else {
                                    raceMap.finishStart = cords
                                    raceMap.update()
                                    sender.sendMessagePr("<green>Локация успешно изменена!</green>")
                                }
                            }
                        }
                        arg("finishEnd") {
                            var coordinates: SimpleCoordinates? = null
                            action {
                                coordinates = null
                                if (sender is Player) coordinates = -(-sender.location)
                            }
                            twoCoordinates {
                                val cords by this
                                action {
                                    coordinates = SimpleCoordinates(cords[0], cords[1])
                                }
                            }

                            lastAction {
                                val cords = coordinates
                                if (cords == null) {
                                    sender.sendMessagePr("<red>Ошибка!</red> <gold>Формат: $label ${args[0]} ${args[1]} ${args[2]} ${args[3]} [координаты(x, z)]</gold>")
                                } else {
                                    raceMap.finishEnd = cords
                                    raceMap.update()
                                    sender.sendMessagePr("<green>Локация успешно изменена!</green>")
                                }
                            }
                        }
                        arg("top1Location") {
                            lastAction {
                                if (sender !is Player) {
                                    sender.sendMessagePr("<red>Эти координаты могут добавлять только игроки</red>")
                                    return@lastAction
                                }

                                raceMap.top1Location = +sender.location
                                raceMap.update()
                                adventure.sender(sender).sendMessagePr("<green>Локация успешно изменена!</green>")

                            }
                        }
                        arg("top2Location") {
                            lastAction {
                                if (sender !is Player) {
                                    sender.sendMessagePr("<red>Эти координаты могут добавлять только игроки</red>")
                                    return@lastAction
                                }

                                raceMap.top2Location = +sender.location
                                raceMap.update()
                                adventure.sender(sender).sendMessagePr("<green>Локация успешно изменена!</green>")

                            }
                        }
                        arg("top3Location") {
                            lastAction {
                                if (sender !is Player) {
                                    sender.sendMessagePr("<red>Эти координаты могут добавлять только игроки</red>")
                                    return@lastAction
                                }

                                raceMap.top3Location = +sender.location
                                raceMap.update()
                                adventure.sender(sender).sendMessagePr("<green>Локация успешно изменена!</green>")

                            }
                        }
                        arg("top1TimeLocation") {
                            lastAction {
                                if (sender !is Player) {
                                    sender.sendMessagePr("<red>Эти координаты могут добавлять только игроки</red>")
                                    return@lastAction
                                }

                                raceMap.top1TimeLocation = +sender.location
                                raceMap.update()
                                adventure.sender(sender).sendMessagePr("<green>Локация успешно изменена!</green>")

                            }
                        }
                        arg("top2TimeLocation") {
                            lastAction {
                                if (sender !is Player) {
                                    sender.sendMessagePr("<red>Эти координаты могут добавлять только игроки</red>")
                                    return@lastAction
                                }

                                raceMap.top2TimeLocation = +sender.location
                                raceMap.update()
                                adventure.sender(sender).sendMessagePr("<green>Локация успешно изменена!</green>")

                            }
                        }
                        arg("top3TimeLocation") {
                            lastAction {
                                if (sender !is Player) {
                                    sender.sendMessagePr("<red>Эти координаты могут добавлять только игроки</red>")
                                    return@lastAction
                                }

                                raceMap.top3TimeLocation = +sender.location
                                raceMap.update()
                                adventure.sender(sender).sendMessagePr("<green>Локация успешно изменена!</green>")

                            }
                        }
                        arg("positionsList") {
                            arg("remove") {
                                inputString {
                                    onTab = {
                                        val list = ArrayList<String>()
                                        raceMap.positionsList.forEach {
                                            list.add("${it.x},${it.y},${it.z}")
                                        }
                                        list
                                    }
                                    val coordinates by this
                                    action1 {
                                        try {
                                            val cords = coordinates.split(",")
                                            if (raceMap.positionsList.removeIf { it.x == cords[0].toDouble() && it.y == cords[1].toDouble() && it.z == cords[2].toDouble() }) {
                                                raceMap.update()
                                                sender.sendMessagePr("<green>Локация успешно удалена!</green>")
                                            }
                                            true
                                        } catch (e: Exception) {
                                            false
                                        }
                                    }
                                }
                                lastAction {
                                    sender.sendMessagePr("<red>Ошибка!</red> <gold>Формат: $label ${args[0]} ${args[1]} ${args[2]} ${args[3]} [координаты(один из вариантов)]</gold>")
                                }
                            }
                            arg("add") {
                                lastAction {
                                    if (sender !is Player) {
                                        sender.sendMessagePr("<red>Эти координаты могут добавлять только игроки</red>")
                                        return@lastAction
                                    }

                                    raceMap.positionsList.add(+sender.location)
                                    raceMap.update()
                                    adventure.sender(sender).sendMessagePr("<green>Локация успешно добавлена!</green>")

                                }
                            }
                            lastAction {
                                sender.sendMessagePr("<red>Ошибка!</red> <gold>Формат: $label ${args[0]} ${args[1]} ${args[2]} [remove/add]</gold>")
                            }
                        }
                        arg("checkpoints") {
                            arg("remove") {
                                inputString {
                                    onTab = {
                                        val list = ArrayList<String>()
                                        raceMap.checkpoints.forEach {
                                            list.add("${it.x},${it.y},${it.z}")
                                        }
                                        list
                                    }
                                    val coordinates by this
                                    action1 {
                                        try {
                                            val cords = coordinates.split(",")
                                            if (raceMap.checkpoints.removeIf { it.x == cords[0].toDouble() && it.y == cords[1].toDouble() && it.z == cords[2].toDouble() }) {
                                                raceMap.update()
                                                sender.sendMessagePr("<green>Локация успешно удалена!</green>")
                                            }
                                            true
                                        } catch (e: Exception) {
                                            false
                                        }
                                    }
                                }
                                lastAction {
                                    sender.sendMessagePr("<red>Ошибка!</red> <gold>Формат: $label ${args[0]} ${args[1]} ${args[2]} ${args[3]} [координаты(один из вариантов)]</gold>")
                                }
                            }
                            arg("add") {
                                lastAction {
                                    if (sender !is Player) {
                                        sender.sendMessagePr("<red>Эти координаты могут добавлять только игроки</red>")
                                        return@lastAction
                                    }

                                    raceMap.checkpoints.add(+sender.location)
                                    raceMap.update()
                                    adventure.sender(sender).sendMessagePr("<green>Локация успешно добавлена!</green>")

                                }
                            }
                            lastAction {
                                sender.sendMessagePr("<red>Ошибка!</red> <gold>Формат: $label ${args[0]} ${args[1]} ${args[2]} [remove/add]</gold>")
                            }
                        }
                        arg("bonuses") {
                            arg("remove") {
                                inputString {
                                    onTab = {
                                        val list = ArrayList<String>()
                                        raceMap.bonuses.forEach { (x, y, z) ->
                                            list.add("${x},${y},${z}")
                                        }
                                        list
                                    }
                                    val coordinates by this
                                    action1 {
                                        try {
                                            val cords = coordinates.split(",")
                                            if (raceMap.bonuses.removeIf { it.x == cords[0].toDouble() && it.y == cords[1].toDouble() && it.z == cords[2].toDouble() }) {
                                                raceMap.update()
                                                sender.sendMessagePr("<green>Бонус успешно удален!</green>")
                                            }
                                            true
                                        } catch (e: Exception) {
                                            false
                                        }
                                    }
                                }
                                lastAction {
                                    sender.sendMessagePr("<red>Ошибка!</red> <gold>Формат: $label ${args[0]} ${args[1]} ${args[2]} ${args[3]} [координаты(один из вариантов)]</gold>")
                                }
                            }
                            arg("add") {
                                var coordinates: Coordinates? = null
                                action {
                                    coordinates = null
                                    if (sender is Player) coordinates = -sender.location
                                }
                                coordinates {
                                    val cords by this
                                    action {
                                        coordinates = Coordinates(cords[0], cords[1], cords[2])
                                    }
                                }

                                lastAction {
                                    if (coordinates == null) {
                                        sender.sendMessagePr("<red>Ошибка!</red> <gold>Формат: $label ${args[0]} ${args[1]} ${args[2]} ${args[3]} [координаты]</gold>")
                                    } else {
                                        raceMap.bonuses.add(coordinates!!)
                                        raceMap.update()
                                        sender.sendMessagePr("<green>Локация успешно добавлена!</green>")
                                    }
                                }
                                lastAction {
                                    sender.sendMessagePr("<red>Ошибка!</red> <gold>Формат: $label ${args[0]} ${args[1]} ${args[2]} add [координаты]</gold>")
                                }
                            }
                            lastAction {
                                sender.sendMessagePr("<red>Ошибка!</red> <gold>Формат: $label ${args[0]} ${args[1]} ${args[2]} [remove/add]</gold>")
                            }
                        }
                        arg("start") {
                            lastAction {
                                val players = Database.availableRaceMaps[raceMap]
                                if (players.isNullOrEmpty()) {
                                    sender.sendMessagePr("<red>Нет игроков для запуска</red>")
                                } else {
                                    try {
                                        raceMap.start(players.toList())
                                    } catch (e: IllegalArgumentException) {
                                        sender.sendMessagePr("<red>Слишком моного игроков</red>")
                                        return@lastAction
                                    }
                                    sender.sendMessagePr("<green>Карта успешно запущена!</green>")
                                }
                            }
                        }
                        arg("activate") {
                            lastAction {
                                raceMap.activate()
                                sender.sendMessagePr("<green>Карта успешно активирована!</green>")
                            }
                        }
                        arg("disable") {
                            lastAction {
                                raceMap.disable()
                                sender.sendMessagePr("<green>Карта успешно отключена!</green>")
                            }
                        }
                        arg("stop") {
                            lastAction {
                                raceMap.stop()
                                sender.sendMessagePr("<green>Карта успешно остановлена!</green>")
                            }
                        }
                    }
                    arg("check", "проверить") {
                        arg("world") {
                            lastAction {
                                sender.sendMessagePr(raceMap.world)
                            }
                        }
                        arg("start") {
                            lastAction {
                                sender.sendMessagePr(raceMap.start.toString())
                            }
                        }
                        arg("end") {
                            lastAction {
                                sender.sendMessagePr(raceMap.end.toString())
                            }
                        }
                        arg("finishStart") {
                            lastAction {
                                sender.sendMessagePr(raceMap.finishStart.toString())
                            }
                        }
                        arg("finishEnd") {
                            lastAction {
                                sender.sendMessagePr(raceMap.finishEnd.toString())
                            }
                        }
                        arg("top1Location") {
                            lastAction {
                                sender.sendMessagePr(raceMap.top1Location.toString())
                            }
                        }
                        arg("top2Location") {
                            lastAction {
                                sender.sendMessagePr(raceMap.top2Location.toString())
                            }
                        }
                        arg("top3Location") {
                            lastAction {
                                sender.sendMessagePr(raceMap.top3Location.toString())
                            }
                        }
                        arg("top1TimeLocation") {
                            lastAction {
                                sender.sendMessagePr(raceMap.top1TimeLocation.toString())
                            }
                        }
                        arg("top2TimeLocation") {
                            lastAction {
                                sender.sendMessagePr(raceMap.top2TimeLocation.toString())
                            }
                        }
                        arg("top3TimeLocation") {
                            lastAction {
                                sender.sendMessagePr(raceMap.top3TimeLocation.toString())
                            }
                        }
                        arg("positionsList") {
                            lastAction {
                                sender.sendMessagePr(raceMap.positionsList.toString())
                            }
                        }
                        arg("checkpoints") {
                            lastAction {
                                sender.sendMessagePr(raceMap.checkpoints.toString())
                            }
                        }
                        arg("bonuses") {
                            lastAction {
                                sender.sendMessagePr(raceMap.bonuses.toString())
                            }
                        }
                        arg("status") {
                            lastAction {
                                Database.startedRaceMaps.forEach {
                                    if (it.raceMap == raceMap) {
                                        sender.sendMessagePr("<aqua>Карта запущена</aqua>")
                                        return@lastAction
                                    }
                                }
                                sender.sendMessagePr("<aqua>Статус: ${raceMap.status}</aqua>")
                            }
                        }
                    }
                }
            }
            arg("setspawn") {
                var location: Location? = null
                action {
                    location = null
                    if (sender is Player) location = sender.location
                }
                coordinates {
                    val coordinates by this
                    action {
                        location = Location(null, coordinates[0], coordinates[1], coordinates[2])
                    }
                    world {
                        val world by this
                        action {
                            location = Location(Bukkit.getWorld(world), coordinates[0], coordinates[1], coordinates[2])
                        }
                    }
                }

                lastAction {
                    val loc = location
                    if (loc == null) {
                        sender.sendMessagePr("<red>Ошибка!</red> <gold>Формат: $label ${args[0]} [координаты] [мир(опционально)]</gold>")
                    } else {
                        mainConfig.spawn = loc
                        sender.sendMessagePr("<green>Точка спавна успешно изменена!")
                    }
                }
            }
            arg("setlobby") {
                var location: Location? = null
                action {
                    location = null
                    if (sender is Player) location = sender.location
                }
                coordinates {
                    val coordinates by this
                    action {
                        location = Location(null, coordinates[0], coordinates[1], coordinates[2])
                    }
                    world {
                        val world by this
                        action {
                            location = Location(Bukkit.getWorld(world), coordinates[0], coordinates[1], coordinates[2])
                        }
                    }
                }

                lastAction {
                    val loc = location
                    if (loc == null) {
                        sender.sendMessagePr("<red>Ошибка!</red> <gold>Формат: $label ${args[0]} [координаты] [мир(опционально)]</gold>")
                    } else {
                        mainConfig.lobby = loc
                        sender.sendMessagePr("<green>Точка лобби успешно изменена!")
                    }
                }
            }
            lastAction {
                sender.sendMessagePr("<red>Ошибка! доступные варианты: /$label [создать, проверить, удалить, setspawn, setlobby]")
            }

        }
    }

    fun ActionsHolder.world(
        openSeparator: String = " ",
        closeSeparator: String = openSeparator,
        onExceptionAction: Action? = null,
        ignoringInnerExit: Boolean = false,
        init: InputArgument<String>.() -> Unit
    ) {
        inputString(
            { Bukkit.getWorlds().map { it.name }.toMutableList() },
            openSeparator, closeSeparator, onExceptionAction, ignoringInnerExit, init
        )
    }

    fun ActionsHolder.twoCoordinates(
        openSeparator: String = "[",
        closeSeparator: String = "]",
        separators: MutableList<String> = mutableListOf(", ", ","),
        onExceptionAction: Action? = null,
        ignoringInnerExit: Boolean = false,
        removeArgAdviceIfUsed: Boolean = false,
        sortAdvicesWithTyped: Boolean = true,
        init: InputListArgument<Double>.() -> Unit
    ) {
        inputDoubleList(
            {
                val target = if (sender is Player) sender.getTargetBlockExact(50) else null
                when (curArgs.fold(StringBuilder()) { builder, str ->
                    builder.append(str)
                }.split(*separators.toTypedArray()).size) {
                    1 -> listOf("x", target?.x?.toString())
                    2 -> listOf("z", target?.z?.toString())
                    else -> emptyList()
                }.filterNotNull().toMutableList()
            },
            2..2,
            openSeparator,
            closeSeparator,
            separators,
            onExceptionAction,
            ignoringInnerExit,
            removeArgAdviceIfUsed,
            sortAdvicesWithTyped,
            init
        )
    }

    fun ActionsHolder.coordinates(
        openSeparator: String = "[",
        closeSeparator: String = "]",
        separators: MutableList<String> = mutableListOf(", ", ","),
        onExceptionAction: Action? = null,
        ignoringInnerExit: Boolean = false,
        removeArgAdviceIfUsed: Boolean = false,
        sortAdvicesWithTyped: Boolean = true,
        init: InputListArgument<Double>.() -> Unit
    ) {
        inputDoubleList(
            {
                val target = if (sender is Player) sender.getTargetBlockExact(50) else null
                when (curArgs.fold(StringBuilder()) { builder, str ->
                    builder.append(str)
                }.split(*separators.toTypedArray()).size) {
                    1 -> listOf("x", target?.x?.toString())
                    2 -> listOf("y", target?.y?.toString())
                    3 -> listOf("z", target?.z?.toString())
                    else -> emptyList()
                }.filterNotNull().toMutableList()
            },
            3..3,
            openSeparator,
            closeSeparator,
            separators,
            onExceptionAction,
            ignoringInnerExit,
            removeArgAdviceIfUsed,
            sortAdvicesWithTyped,
            init
        )
    }
}
