package ru.batr.sdboat.racemap

import org.bukkit.*
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.block.BlockEvent
import org.bukkit.event.block.BlockFormEvent
import org.bukkit.event.block.BlockFromToEvent
import org.bukkit.event.player.PlayerDropItemEvent
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.event.player.PlayerMoveEvent
import org.bukkit.event.player.PlayerQuitEvent
import org.bukkit.event.vehicle.VehicleEnterEvent
import org.bukkit.event.vehicle.VehicleExitEvent
import ru.batr.sdboat.SDBoat
import ru.batr.sdboat.SDBoat.Companion.mainConfig
import ru.batr.sdboat.SDBoat.Companion.sendMessagePr
import ru.batr.sdboat.database.Database

class RaceMapsHandler : Listener {
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    fun PlayerQuitEvent.onLeave() {
        synchronized(SDBoat.instance) {
            val player = player
            val playerUUID = player.uniqueId
            Database.executingPlayers.add(playerUUID)
            for (raceMap in Database.startedRaceMaps.toList()) {
                if (raceMap.players.contains(playerUUID)) {
                    Database.startedRaceMaps.find { it.raceMap == raceMap.raceMap }!!.playerLose(playerUUID)
                }
            }
            for ((raceMap, players) in Database.availableRaceMaps.toMap()) {
                if (players.contains(playerUUID)) {
                    val players1 = players.toMutableSet()
                    players1.remove(playerUUID)
                    player.teleport(mainConfig.spawn)
                    Database.availableRaceMaps[raceMap] = players1
                }
            }
            for (raceMap in Database.waitingMaps.toList()) {
                if (raceMap.players.contains(playerUUID)) {
                    Database.waitingMaps.find { it.raceMap == raceMap.raceMap }!!.playerLose(playerUUID)
                }
            }
            Database.executingPlayers.remove(playerUUID)
        }
    }

    @EventHandler
    fun VehicleEnterEvent.onBoatEnter() {
        vehicle.passengers.filterNotNull().forEach { player ->
            if (player is Player) {
                val playerUUID = player.uniqueId
                if (Database.executingPlayers.contains(playerUUID)) return
                Bukkit.getScheduler().runTaskLater(SDBoat.instance, Runnable {
                    for (raceMap in Database.startedRaceMaps) {
                        if (raceMap.players.contains(playerUUID)) {
                            raceMap.kill(playerUUID, vehicle)
                            return@Runnable
                        }
                    }
                    for (raceMap in Database.waitingMaps) {
                        if (raceMap.players.contains(playerUUID)) {
                            raceMap.kill(playerUUID, vehicle)
                            return@Runnable
                        }
                    }
                    for (raceMap in Database.raceMaps) {
                        if (player.location in raceMap.start to raceMap.end) {
                            vehicle.remove()
                        }
                    }
                }, 10)
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    fun VehicleExitEvent.onBoatLeave() {
        synchronized(SDBoat.instance) {
            val player = exited
            if (player is Player) {
                val playerUUID = player.uniqueId
                if (Database.executingPlayers.contains(playerUUID)) return
                Bukkit.getScheduler().runTaskLater(SDBoat.instance, Runnable {
                    for (raceMap in Database.startedRaceMaps) {
                        if (raceMap.players.contains(playerUUID)) {
                            raceMap.kill(playerUUID, vehicle)
                            return@Runnable
                        }
                    }
                    for (raceMap in Database.waitingMaps) {
                        if (raceMap.players.contains(playerUUID)) {
                            raceMap.kill(playerUUID, vehicle)
                            return@Runnable
                        }
                    }
                    for (raceMap in Database.raceMaps) {
                        if (player.location in raceMap.start to raceMap.end) {
                            vehicle.remove()
                        }
                    }
                }, 10)

            }
        }
    }

    @EventHandler
    fun PlayerDropItemEvent.onDrop() {
        for (raceMap in Database.startedRaceMaps) {
            if (raceMap.players.contains(player.uniqueId)) {
                isCancelled = true
            }
        }
        for (raceMap in Database.waitingMaps) {
            if (raceMap.players.contains(player.uniqueId)) {
                isCancelled = true
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    fun PlayerInteractEvent.onItemUse() {
        val item = this.item
        for (raceMap in Database.startedRaceMaps) {
            if (raceMap.players.contains(player.uniqueId)) {
                if (item != null) Bonus.entries.forEach {
                    if (it.check(item)) {
                        item.amount -= 1
                        it.activate(raceMap, player)
                    }
                }
                isCancelled = true
            }
        }
        for (raceMap in Database.waitingMaps) {
            if (raceMap.players.contains(player.uniqueId)) {
                isCancelled = true
            }
        }
    }

    @EventHandler
    fun PlayerMoveEvent.onMove() {
        synchronized(SDBoat.instance) {
            val player = player
            val playerUUID = player.uniqueId
            if (Database.executingPlayers.contains(playerUUID)) return
            for (raceMap in Database.waitingMaps) {
                if (raceMap.players.contains(playerUUID)) isCancelled = true
            }
            for (raceMap in Database.startedRaceMaps.toList()) {
                if (raceMap.players.contains(playerUUID)) {
                    if (player.location in raceMap.raceMap.finishStart to raceMap.raceMap.finishEnd) {
                        raceMap.playerFinished(playerUUID)
                    }
                    if (player.location !in raceMap.raceMap.start to raceMap.raceMap.end) {
                        player.sendMessagePr(mainConfig.mapLeave)
                        raceMap.kill(playerUUID)
                    }
                    for (checkpoint in raceMap.raceMap.checkpoints) {
                        if (player.location in -(-checkpoint)..mainConfig.checkpointRange) {
                            if (+raceMap.checkpoints[playerUUID]!! != checkpoint) {
                                raceMap.checkpoints[playerUUID] = +checkpoint
                                player.world.spawnParticle(Particle.FIREWORKS_SPARK, player.location, 10)
                                player.world.spawnParticle(
                                    Particle.FIREWORKS_SPARK,
                                    player.location.clone().add(0.0, 1.0, 0.0),
                                    30
                                )
                                player.sendMessagePr(mainConfig.checkPointMessage)
                            }
                        }
                    }
                    for ((entity, bonus) in raceMap.bonuses.toMap()) {
                        if (player.location in -(-entity.location)..mainConfig.bonusRange) {
                            player.inventory.addItem(bonus.item)
                            raceMap.bonuses.remove(entity to bonus)
                            entity.remove()
                            player.world.spawnParticle(Particle.FIREWORKS_SPARK, player.location, 10)
                            player.world.spawnParticle(
                                Particle.FIREWORKS_SPARK,
                                player.location.clone().add(0.0, 1.0, 0.0),
                                30
                            )
                        }
                    }
                    val loc = player.location
                    for (y in loc.y.toInt() downTo loc.y.toInt() - 3) {
                        val block = player.world.getBlockAt(Location(loc.world, loc.x, y.toDouble(), loc.z))
                        if (!block.isEmpty) {
                            if (block.type == Material.MAGMA_BLOCK) {
                                raceMap.kill(playerUUID)
                            }
                        }
                    }
                }
            }
        }
    }

    operator fun SimpleCoordinates.rangeTo(int: Int) =
        SimpleCoordinates(x - int, z - int) to SimpleCoordinates(x + int, z + int)
}

