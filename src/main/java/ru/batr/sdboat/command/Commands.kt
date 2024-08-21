package ru.batr.sdboat.command

import org.bukkit.Bukkit
import org.jetbrains.exposed.exceptions.ExposedSQLException
import ru.batr.sdboat.SDBoat.Companion.sendMessagePr
import ru.batr.sdboat.SDBoat.Companion.sendMessage
import ru.batr.sdboat.database.Database
import ru.batr.sdboat.racemap.RaceMap

object Commands {
    fun init() {
        command("boatracemanage") {
            arg("create", "создать") {
                inputString(+"имяКарты") {
                    val name by this
                    inputString({ Bukkit.getWorlds().map { it.name }.toMutableList() }) {
                        val world by this
                        inputIntList(+"координаты1", 2..2) {
                            val firstCoordinate by this
                            inputIntList(+"координаты2", 2..2) {
                                val secondCoordinate by this
                                lastAction {
                                    try {
                                        Database.addRaceMap(
                                            name,
                                            RaceMap(
                                                world,
                                                firstCoordinate[0] to firstCoordinate[1],
                                                secondCoordinate[0] to secondCoordinate[1]
                                            )
                                        )
                                        sender.sendMessagePr("<green>Карта успешно создана!</green>")
                                    } catch (e: ExposedSQLException) {
                                        sender.sendMessagePr("<red>Такая карта уже существует!</red>")
                                    } catch (e: IndexOutOfBoundsException) {
                                        sender.sendMessagePr("<red>Неправильно введены координаты!</red>")
                                    }
                                }
                            }
                            lastAction {
                                sender.sendMessagePr("<red>Ошибка! неправильно введены вторые координаты координаты</red>")
                            }
                        }
                        lastAction {
                            sender.sendMessagePr("<red>Ошибка! неправильно введены первые координаты координаты</red>")
                        }
                    }
                }
            }
            arg("check", "проверить") {
                lastAction {
                    sender.sendMessagePr("Карты:")
                    Database.raceMaps.forEach { (mapName, map) ->
                        sender.sendMessage("<aqua>$mapName <grey>=</grey> $map</aqua>")
                    }
                }
            }
            arg("delete", "удалить") {
                inputString({ Database.raceMaps.keys.toMutableList() }) {
                    val mapName by this
                    lastAction {
                        if (Database.raceMaps.keys.contains(mapName)) {
                            Database.removeRaceMap(mapName)
                            sender.sendMessagePr("<green>Карта <aqua>$mapName</aqua> успешно удалена!</green>")
                        } else {
                            sender.sendMessagePr("<red>Ошибка! Карта не найдена</red>")
                        }
                    }
                }
            }
            lastAction {
                sender.sendMessagePr("<red>Ошибка! доступные варианты: /$label [создать] [проверить] [удалить]")
            }

        }
    }
}