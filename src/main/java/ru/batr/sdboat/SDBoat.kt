package ru.batr.sdboat

import TextFormatter
import net.kyori.adventure.audience.Audience
import net.kyori.adventure.platform.bukkit.BukkitAudiences
import net.kyori.adventure.platform.bukkit.BukkitComponentSerializer
import net.kyori.adventure.text.Component
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.World
import org.bukkit.command.CommandSender
import org.bukkit.entity.ArmorStand
import org.bukkit.entity.Entity
import org.bukkit.entity.EntityType
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.meta.SkullMeta
import org.bukkit.plugin.java.JavaPlugin
import ru.batr.sdboat.command.MainCommand
import ru.batr.sdboat.command.ManageCommand
import ru.batr.sdboat.config.MainConfig
import ru.batr.sdboat.database.Database
import ru.batr.sdboat.racemap.*
import java.util.UUID

class SDBoat : JavaPlugin() {

    companion object {
        lateinit var instance: SDBoat
            private set
        lateinit var mainConfig: MainConfig
            private set
        val NPCs: MutableList<Entity> = ArrayList()
        val topUpdater by lazy {
            Bukkit.getScheduler().scheduleSyncRepeatingTask(instance, {
                NPCs.forEach { it.remove() }
                Database.raceMaps.forEach { raceMap ->
                    val world = Bukkit.getWorld(raceMap.world)!!
                    raceMap.topPlayers.toList().sortedByDescending { it.second.wins }.forEachIndexed { idx, (uuid, top) ->
                        val i = idx + 1
                        val playerUUID = UUID.fromString(uuid)
                        when(i) {
                            1 -> {
                                raceMap.top1Location?.let { loc ->
                                    spawnNPC(top, playerUUID, loc, world, 1)
                                }
                            }
                            2 -> {
                                raceMap.top2Location?.let { loc ->
                                    spawnNPC(top, playerUUID, loc, world, 2)
                                }
                            }
                            3 -> {
                                raceMap.top3Location?.let { loc ->
                                    spawnNPC(top, playerUUID, loc, world, 3)
                                }
                            }
                        }
                    }
                    raceMap.topPlayers.toList().sortedBy { it.second.topRace ?: Double.MAX_VALUE }.forEachIndexed { idx, (uuid, top) ->
                        val i = idx + 1
                        val playerUUID = UUID.fromString(uuid)

                        when(i) {
                            1 -> {
                                raceMap.top1TimeLocation?.let { loc ->
                                    spawnNPC(top, playerUUID, loc, world, 1)
                                }
                            }
                            2 -> {
                                raceMap.top2TimeLocation?.let { loc ->
                                    spawnNPC(top, playerUUID, loc, world, 2)
                                }
                            }
                            3 -> {
                                raceMap.top3TimeLocation?.let { loc ->
                                    spawnNPC(top, playerUUID, loc, world, 3)
                                }
                            }
                        }
                    }
                }
            }, 0, mainConfig.topUpdateTime * 20L)
        }
        val adventure by lazy {
            BukkitAudiences.create(instance)
        }
        fun Audience.sendMessagePr(component: Component) {
            sendMessage(mainConfig.prefix.append(Component.text(" ")).append(component))
        }

        fun Audience.sendMessagePr(message: String) {
            sendMessagePr(TextFormatter.format(message))
        }

        fun Audience.sendMessage(message: String) {
            sendMessage(TextFormatter.format(message))
        }

        fun CommandSender.sendMessage(component: Component) {
            adventure.sender(this).sendMessage(component)
        }

        fun CommandSender.sendMessagePr(message: String) {
            adventure.sender(this).sendMessagePr(TextFormatter.format(message))
        }
        fun CommandSender.sendMessagePr(component: Component) {
            adventure.sender(this).sendMessagePr(component)
        }
        fun spawnNPC(top: TopPlayer, playerUUID: UUID, cords: ExactCoordinates, world: World, place: Int) {
            val loc = +cords
            loc.world = world
            world.getNearbyEntities(loc, 2.0, 2.0, 2.0).sortedBy { it is ArmorStand }.forEach { it.remove() }
            val armorStand = world.spawnEntity(loc, EntityType.ARMOR_STAND) as ArmorStand
            armorStand.setArms(true)
            armorStand.setBasePlate(false)
            armorStand.equipment!!.boots = when(place) {
                1 -> ItemStack(Material.GOLDEN_BOOTS)
                2 -> ItemStack(Material.IRON_BOOTS)
                3 -> ItemStack(Material.LEATHER_BOOTS)
                else -> null
            }
            armorStand.equipment!!.leggings = when(place) {
                1 -> ItemStack(Material.GOLDEN_LEGGINGS)
                2 -> ItemStack(Material.IRON_LEGGINGS)
                3 -> ItemStack(Material.LEATHER_LEGGINGS)
                else -> null
            }
            armorStand.equipment!!.chestplate = when(place) {
                1 -> ItemStack(Material.GOLDEN_CHESTPLATE)
                2 -> ItemStack(Material.IRON_CHESTPLATE)
                3 -> ItemStack(Material.LEATHER_CHESTPLATE)
                else -> null
            }
            val head = ItemStack(Material.PLAYER_HEAD)
            val meta = head.itemMeta as SkullMeta
            meta.owningPlayer = Bukkit.getOfflinePlayer(playerUUID)
            head.itemMeta = meta
            armorStand.equipment!!.helmet = head
            val color = when(place) {
                1 -> "yellow"
                2 -> "gray"
                3 -> "gold"
                else -> "dark_aqua"
            }
            armorStand.customName = BukkitComponentSerializer.legacy().serialize(TextFormatter.format("<$color><dark_gray>[</dark_gray>$place<dark_gray>]</dark_gray> ${Bukkit.getOfflinePlayer(playerUUID).name!!}  ${top.wins} ${top.loses} ${top.topRace?.toTime()}</$color>"))
            armorStand.isCustomNameVisible = true
            NPCs.add(armorStand)

        }
    }

    override fun onEnable() {
        instance = this
        mainConfig = MainConfig()
        server.pluginManager.registerEvents(RaceMapsHandler(), this)
        Database.connect()
        ManageCommand.init()
        MainCommand.init()
        topUpdater

        server.consoleSender.sendMessagePr("<green>Plugin Enabled!</green>")
    }

    override fun onDisable() {
        Bukkit.getScheduler().cancelTask(topUpdater)
        while ( Database.startedRaceMaps.isNotEmpty()) {
            try {
                Database.startedRaceMaps.toList().forEach { it.stop() }
            } catch (_: Exception) {
                logger.warning("Problems with disabling")
                Thread.sleep(300)
            }
        }
        NPCs.forEach { it.remove() }
        server.consoleSender.sendMessagePr("<aqua>Plugin disabled!</aqua>")
    }


}