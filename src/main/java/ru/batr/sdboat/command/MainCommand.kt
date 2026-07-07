package ru.batr.sdboat.command

import net.kyori.adventure.audience.Audience
import org.bukkit.Bukkit
import org.bukkit.entity.Player
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
                var targets: Set<Player>? = null
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
                    inputPlayersListSelector {
                        val players by this
                        action {
                            if (sender.hasPermission("sdboat.admin")) {
                                targets = players.flatten().toSet()
                            }
                        }
                    }
                }
                action {
                    try {
                        if (targets == null) {
                            if (sender !is Player) {
                                sender.sendMessagePr("<red>Укажите кого поставить в очередь</red>")
                                return@action
                            }
                            targets = setOf(sender)
                        }
                        if (raceMap == null) Database.raceMaps.firstOrNull()
                        targets?.forEach { player ->
                            raceMap?.addPlayerToQueue(player.uniqueId)
                        }
                    } catch (e: Exception) {
                        sender.sendMessage("ошибка ${e.message}")
                        e.printStackTrace()
                    }
//                    sender.sendMessagePr("<red>Ошибка! Не удалось подключиться</red>")
//                    try {
//                        Database.availableRaceMaps[raceMap]!!.remove((sender as Player).uniqueId)
//                    } catch (_: Exception) {
//                    }
                }
                lastAction {
                    raceMap = null
                    targets = null
                }
            }
            arg("leave") {
                var targets: Set<Player>? = null
                inputPlayersListSelector {
                    val players by this
                    action {
                        if (sender.hasPermission("sdboat.admin")) {
                            targets = players.flatten().toSet()
                        }
                    }
                }
                action {
                    if (targets == null) {
                        if (sender !is Player) {
                            sender.sendMessagePr("<red>Покинуть в очередь может только игрок</red>")
                            return@action
                        }
                        targets = setOf(sender)
                    }
                    targets?.forEach { player ->
                        for (raceMap in Database.startedRaceMaps.toList()) {
                            if (raceMap.players.contains(player.uniqueId)) {
                                raceMap.playerLose(player.uniqueId)
                                player.sendMessagePr(mainConfig.gameLeave)
                                sender.sendMessagePr("${player.name} исключён из игры")
                            }
                        }
                        for ((raceMap, players) in Database.availableRaceMaps.toMap()) {
                            if (players.contains(player.uniqueId)) {
                                val players1 = players.toMutableSet()
                                players1.remove(player.uniqueId)
                                Database.availableRaceMaps[raceMap] = players1
                                player.teleport(mainConfig.spawn)
                                player.sendMessagePr(mainConfig.queueLeave)
                                sender.sendMessagePr("${player.name} исключён из игры")
                            }
                        }
                    }
                }
                lastAction {
                    targets = null
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
                        sender.sendTop(raceMap.topPlayers.toList().sortedByDescending { it.second.wins })
                        sender.sendMessagePr("<gold>Топ по времени:</gold>")
                        sender.sendTop(raceMap.topPlayers.toList().sortedBy { it.second.topRace ?: Double.MAX_VALUE })
                    }
                }
                lastAction {
                    val topPlayers =
                        Database.raceMaps.flatMap { it.topPlayers.toList() }.groupBy { it.first }.map { (name, list) ->
                            name to list.fold(TopPlayer()) { top1, top2 -> top1 + top2.second }
                        }
                    sender.sendMessagePr("<gold>Топ:</gold> <grey>[</grey><aqua>Побед</aqua><grey>]</grey> <grey>[</grey><aqua>Поражений</aqua><grey>]</grey> <grey>[</grey><aqua>Лучшее время</aqua><grey>]</grey>")
                    sender.sendTop(topPlayers.toList().sortedByDescending { it.second.wins })
                    sender.sendMessagePr("<gold>Топ по времени:</gold>")
                    sender.sendTop(topPlayers.toList().sortedBy { it.second.topRace ?: Double.MAX_VALUE })

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
                1 -> sendMessagePr(
                    "<yellow><dark_gray>[</dark_gray>$i<dark_gray>]</dark_gray> ${
                        Bukkit.getOfflinePlayer(
                            playerUUID
                        ).name!!
                    }  ${top.wins} ${top.loses} ${top.topRace?.toTime()}</yellow>"
                )

                2 -> sendMessagePr(
                    "<gray><dark_gray>[</dark_gray>$i<dark_gray>]</dark_gray> ${
                        Bukkit.getOfflinePlayer(
                            playerUUID
                        ).name!!
                    }  ${top.wins} ${top.loses} ${top.topRace?.toTime()}</gray>"
                )

                3 -> sendMessagePr(
                    "<light_purple><dark_gray>[</dark_gray>$i<dark_gray>]</dark_gray> ${
                        Bukkit.getOfflinePlayer(
                            playerUUID
                        ).name!!
                    }  ${top.wins} ${top.loses} ${top.topRace?.toTime()}</light_purple>"
                )

                in 4..5 -> sendMessagePr(
                    "<gold><dark_gray>[</dark_gray>$i<dark_gray>]</dark_gray> $${
                        Bukkit.getOfflinePlayer(
                            playerUUID
                        ).name!!
                    }  ${top.wins} ${top.loses} ${top.topRace?.toTime()}</gold>"
                )

                else -> {
                    if (this is Player && playerUUID == this.uniqueId) {
                        if (i > 6) sendMessagePr("<gray>...</gray>")
                        sendMessagePr("<dark_aqua><dark_gray>[</dark_gray>$i<dark_gray>]</dark_gray> $name  ${top.wins} ${top.loses} ${top.topRace?.toTime()}</dark_aqua>")
                    }
                }
            }
        }
    }

}