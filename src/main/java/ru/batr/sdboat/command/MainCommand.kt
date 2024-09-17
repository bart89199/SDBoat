package ru.batr.sdboat.command

import net.kyori.adventure.audience.Audience
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import ru.batr.sdboat.SDBoat.Companion.adventure
import ru.batr.sdboat.SDBoat.Companion.mainConfig
import ru.batr.sdboat.SDBoat.Companion.sendMessagePr
import ru.batr.sdboat.database.Database
import ru.batr.sdboat.racemap.*
import java.util.*

object MainCommand {
    fun init() {
        command("boatrace") {
            arg("join") {
                var raceMap: RaceMap? = null
                inputString {
                    onTab = { Database.raceMaps.map { it.name }.toMutableList() }
                    val mapName by this
                    action1 {
                        raceMap = Database.availableRaceMaps.keys.find { it.name == mapName }.let {
                            if (it == null) {
                                sender.sendMessagePr("<red>Ошибка! Карта не найдена</red>")
                                return@action1 true
                            }
                            it
                        }
                        false
                    }
                }
                lastAction {
                    try {
                        if (sender !is Player) {
                            sender.sendMessagePr("<red>Встать в очередь может только игрок</red>")
                            return@lastAction
                        }
                        if (raceMap == null) {
                            Database.raceMaps.forEach {
                                if (it.addPlayerToQueue(sender.uniqueId)) return@lastAction
                            }
                        } else {
                            if (raceMap!!.addPlayerToQueue(sender.uniqueId)) return@lastAction
                        }
                    } catch (_: Exception) {
                    }
                    sender.sendMessagePr("<red>Ошибка! Неудалось подключиться</red>")
                    try {
                        Database.availableRaceMaps[raceMap]!!.remove((sender as Player).uniqueId)
                    } catch (_: Exception) {
                    }
                }
            }
            arg("leave") {
                lastAction {
                    if (sender !is Player) {
                        sender.sendMessagePr("<red>Покинуть в очередь может только игрок</red>")
                        return@lastAction
                    }
                    for (raceMap in Database.startedRaceMaps.toList()) {
                        if (raceMap.players.contains(sender.uniqueId)) {
                            sender.teleport(mainConfig.spawn)
                            raceMap.playerLose(sender.uniqueId)
                            (adventure.sender(sender)).sendMessagePr(mainConfig.gameLeave)
                            return@lastAction
                        }
                    }
                    for ((raceMap, players) in Database.availableRaceMaps.toMap()) {
                        if (players.contains(sender.uniqueId)) {
                            val players1 = players.toMutableSet()
                            players1.remove(sender.uniqueId)
                            Database.availableRaceMaps[raceMap] = players1
                            sender.teleport(mainConfig.spawn)
                            (adventure.sender(sender)).sendMessagePr(mainConfig.queueLeave)

                            return@lastAction
                        }
                    }

                    (adventure.sender(sender)).sendMessagePr("<red>Вы и так не в очереди</red>")

                }
            }
            arg("top") {
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
                    lastAction {
                        sender.sendMessagePr("<gold>Топ:</gold> <grey>[</grey><aqua>Побед</aqua><grey>]</grey> <grey>[</grey><aqua>Поражений</aqua><grey>]</grey> <grey>[</grey><aqua>Лучшее время</aqua><grey>]</grey>")
                        adventure.sender(sender).sendTop(raceMap.topPlayers.toList().sortedByDescending { it.second.wins })
                        sender.sendMessagePr("<gold>Топ по времени:</gold>")
                        adventure.sender(sender).sendTop(raceMap.topPlayers.toList().sortedBy { it.second.topRace ?: Double.MAX_VALUE })
                    }
                }
                lastAction {
                    val topPlayers =
                        Database.raceMaps.flatMap { it.topPlayers.toList() }.groupBy { it.first }.map { (name, list) ->
                            name to list.fold(TopPlayer()) { top1, top2 -> top1 + top2.second }
                        }
                    sender.sendMessagePr("<gold>Топ:</gold> <grey>[</grey><aqua>Побед</aqua><grey>]</grey> <grey>[</grey><aqua>Поражений</aqua><grey>]</grey> <grey>[</grey><aqua>Лучшее время</aqua><grey>]</grey>")
                    adventure.sender(sender).sendTop(topPlayers.toList().sortedByDescending { it.second.wins })
                    sender.sendMessagePr("<gold>Топ по времени:</gold>")
                    adventure.sender(sender).sendTop(topPlayers.toList().sortedBy { it.second.topRace ?: Double.MAX_VALUE })

                }
            }
            lastAction {
                sender.sendMessagePr("<aqua>Возможные аргументы: [join, top, leave]</aqua>")
            }
        }
    }

    fun Audience.sendTop(topPlayers: Iterable<Pair<String, TopPlayer>>) {
        topPlayers.forEachIndexed { idx, (uuid, top) ->
            val playerUUID = UUID.fromString(uuid)
            val i = idx + 1
            when (i) {
                1 -> sendMessagePr("<yellow><dark_gray>[</dark_gray>$i<dark_gray>]</dark_gray> ${Bukkit.getOfflinePlayer(playerUUID).name!!}  ${top.wins} ${top.loses} ${top.topRace?.toTime()}</yellow>")
                2 -> sendMessagePr("<gray><dark_gray>[</dark_gray>$i<dark_gray>]</dark_gray> ${Bukkit.getOfflinePlayer(playerUUID).name!!}  ${top.wins} ${top.loses} ${top.topRace?.toTime()}</gray>")
                3 -> sendMessagePr("<light_purple><dark_gray>[</dark_gray>$i<dark_gray>]</dark_gray> ${Bukkit.getOfflinePlayer(playerUUID).name!!}  ${top.wins} ${top.loses} ${top.topRace?.toTime()}</light_purple>")
                in 4..5 -> sendMessagePr("<gold><dark_gray>[</dark_gray>$i<dark_gray>]</dark_gray> $${Bukkit.getOfflinePlayer(playerUUID).name!!}  ${top.wins} ${top.loses} ${top.topRace?.toTime()}</gold>")
                else -> {
                    if (this is Player && playerUUID == this.uniqueId) {
                        if (i > 6) adventure.sender(this).sendMessagePr("<gray>...</gray>")
                        adventure.sender(this).sendMessagePr("<dark_aqua><dark_gray>[</dark_gray>$i<dark_gray>]</dark_gray> $name  ${top.wins} ${top.loses} ${top.topRace?.toTime()}</dark_aqua>")
                    }
                }
            }
        }
    }
}