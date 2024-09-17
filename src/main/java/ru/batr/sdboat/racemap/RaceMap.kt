package ru.batr.sdboat.racemap

import TextFormatter
import kotlinx.serialization.Serializable
import net.kyori.adventure.title.Title
import org.bukkit.*
import org.bukkit.entity.ArmorStand
import org.bukkit.entity.Boat
import org.bukkit.entity.Entity
import org.bukkit.entity.EntityType
import org.bukkit.scheduler.BukkitTask
import ru.batr.sdboat.SDBoat
import ru.batr.sdboat.SDBoat.Companion.adventure
import ru.batr.sdboat.SDBoat.Companion.mainConfig
import ru.batr.sdboat.SDBoat.Companion.sendMessagePr
import ru.batr.sdboat.database.Database
import java.util.*
import java.util.concurrent.ThreadLocalRandom
import kotlin.collections.ArrayList
import kotlin.collections.HashMap
import kotlin.collections.HashSet
import kotlin.math.min

@Serializable
data class SimpleCoordinates(val x: Double, val z: Double)

@Serializable
data class Coordinates(val x: Double, val y: Double, val z: Double)

@Serializable
data class ExactCoordinates(val x: Double, val y: Double, val z: Double, val yaw: Float, val pitch: Float)

@Serializable
data class RaceMap(
    val name: String,
    var world: String,
    var start: SimpleCoordinates,
    var end: SimpleCoordinates,
    var finishStart: SimpleCoordinates,
    var finishEnd: SimpleCoordinates,
    var top1Location: ExactCoordinates? = null,
    var top2Location: ExactCoordinates? = null,
    var top3Location: ExactCoordinates? = null,
    var top1TimeLocation: ExactCoordinates? = null,
    var top2TimeLocation: ExactCoordinates? = null,
    var top3TimeLocation: ExactCoordinates? = null,
    val positionsList: MutableList<ExactCoordinates> = ArrayList(),
    val checkpoints: MutableList<ExactCoordinates> = ArrayList(),
    val bonuses: MutableList<Coordinates> = ArrayList(),
    var status: MapStatus = MapStatus.AVAILABLE,
    val topPlayers: MutableMap<String, TopPlayer> = HashMap(),
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is RaceMap) return false

        if (name != other.name) return false
        return true
    }

    override fun hashCode(): Int {
        return name.hashCode()
    }
}

@Serializable
data class TopPlayer(var wins: Int = 0, var loses: Int = 0, var topRace: Double? = null)

data class StartedRaceMap(
    var raceMap: RaceMap,
    val players: MutableList<UUID>,
    var startTime: Long,
    val bonuses: MutableList<Pair<Entity, Bonus>> = ArrayList(),
    val checkpoints: MutableMap<UUID, Location> = HashMap(),
    var nextPlace: Int = 1,
    var tlTask: BukkitTask? = null,
    var bonusesTask: Int? = null,
    var stoped: Boolean = false,
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is StartedRaceMap) return false

        if (raceMap != other.raceMap) return false

        return true
    }

    override fun hashCode(): Int {
        return raceMap.hashCode()
    }
}

fun Double.toTime(): String {
    val min = this.toInt() / 60
    val sec = this.toInt() % 60
    return "${if (min < 10) "0$min" else min}:${if (sec < 10) "0$sec" else sec}"
}

operator fun TopPlayer.plus(other: TopPlayer) = TopPlayer(
    wins + other.wins,
    loses + other.loses,
    min(
        topRace ?: Double.MAX_VALUE,
        other.topRace ?: Double.MAX_VALUE
    ).let { if (it == Double.MAX_VALUE) null else it })
operator fun UUID.unaryPlus() = Bukkit.getOfflinePlayer(this)
operator fun Location.unaryPlus() = ExactCoordinates(x, y, z, yaw, pitch)
operator fun Location.unaryMinus() = Coordinates(x, y, z)
operator fun Coordinates.unaryPlus() = Location(null, x, y, z)
operator fun Coordinates.unaryMinus() = SimpleCoordinates(x, z)
operator fun ExactCoordinates.unaryMinus() = Coordinates(x, y, z)
operator fun ExactCoordinates.unaryPlus() =
    Location(null, x, y, z, yaw, pitch)

operator fun Pair<SimpleCoordinates, SimpleCoordinates>.contains(location: Location): Boolean {
    val xr = if (first.x < second.x) first.x..second.x else second.x..first.x
    val zr = if (first.z < second.z) first.z..second.z else second.z..first.z
    val lx = location.x
    val lz = location.z
    return lx in xr && lz in zr
}

fun RaceMap.update() {
    Database.updateRaceMap(name, this)
}

fun StartedRaceMap.placePlayerInBoat(playerUUID: UUID, location: Location, hasOld: Boolean = true) {
    Database.executingPlayers.add(playerUUID)
    val offlinePlayer = +playerUUID
    synchronized(SDBoat.instance) {
        val player = offlinePlayer.player ?: return@synchronized
        val vehicle = player.player?.vehicle
        if (hasOld && vehicle is Boat) return@synchronized
        vehicle?.removePassenger(player)
        if (hasOld) {
            vehicle?.remove()
        }
        val newWorld = location.world ?: Bukkit.getWorld(raceMap.world) ?: player.world
        location.world = newWorld
        player.teleport(location)
        val boat = newWorld.spawnEntity(location, EntityType.BOAT) as Boat
        boat.addPassenger(player)
    }
    Database.executingPlayers.remove(playerUUID)
}

fun StartedRaceMap.removePlayer(playerUUID: UUID) {
    val offlinePlayer = +playerUUID
    Database.executingPlayers.add(playerUUID)
    val vehicle = offlinePlayer.player?.vehicle
    vehicle?.remove()
    justRemovePlayer(playerUUID)
    if (!offlinePlayer.isOnline) return
    offlinePlayer.player?.teleport(mainConfig.spawn)
    Database.executingPlayers.remove(playerUUID)
}

fun StartedRaceMap.justRemovePlayer(playerUUID: UUID) {
    val offlinePlayer = +playerUUID
    synchronized(SDBoat.instance) {
        Bonus.entries.forEach {
            offlinePlayer.player?.let { player ->
                player.inventory.toList().forEach { item ->
                    if (item != null && !item.type.isAir && it.check(item)) player.inventory.remove(item)
                }
            }
        }
        players.remove(playerUUID)
        checkpoints.remove(playerUUID)
        if (players.isEmpty()) raceMap.stop()
    }
}

fun StartedRaceMap.kill(playerUUID: UUID, vehicle: Entity? = (+playerUUID).player?.vehicle) {
    val offlinePlayer = +playerUUID

    if (!offlinePlayer.isOnline) {
        playerLose(playerUUID)
        return
    }
    synchronized(SDBoat.instance) {
        Database.executingPlayers.add(playerUUID)
        vehicle?.remove()
        placePlayerInBoat(playerUUID, checkpoints[playerUUID]!!)
        Database.executingPlayers.remove(playerUUID)
    }
}

fun StartedRaceMap.playerFinished(playerUUID: UUID) {
    val offlinePlayer = +playerUUID
    if (!offlinePlayer.isOnline) return
    synchronized(SDBoat.instance) {
        val place: Int = this.nextPlace
        this.nextPlace++
        val time = (System.currentTimeMillis() - startTime) / 1000.0
        offlinePlayer.player?.sendMessagePr(mainConfig.finishMessage(place, time.toTime()))
        val topPlayer = raceMap.topPlayers[playerUUID.toString()] ?: TopPlayer()
        if (place <= mainConfig.winPlaces) topPlayer.wins++ else topPlayer.loses++
        topPlayer.topRace = min(time, topPlayer.topRace ?: Double.MAX_VALUE)
        raceMap.topPlayers[playerUUID.toString()] = topPlayer
        raceMap.update()
        offlinePlayer.player?.let { it.playSound(it, Sound.ENTITY_FIREWORK_ROCKET_BLAST, 20f, 0f) }
        offlinePlayer.player?.let {
            it.world.spawnParticle(Particle.FIREWORKS_SPARK, it.location.clone().add(0.0, 1.0, 0.0), 60)
        }
        justRemovePlayer(playerUUID)
        Bukkit.getScheduler().runTaskLater(SDBoat.instance, Runnable {
            removePlayer(playerUUID)
        }, 40)
    }
}

fun StartedRaceMap.playerLose(playerUUID: UUID) {
    synchronized(SDBoat.instance) {
        val topPlayer = raceMap.topPlayers[playerUUID.toString()] ?: TopPlayer()
        topPlayer.loses++
        raceMap.topPlayers[playerUUID.toString()] = topPlayer
        raceMap.update()
        removePlayer(playerUUID)
    }
}

fun RaceMap.stop() {
    synchronized(SDBoat.instance) {
        val maps = Database.startedRaceMaps.toMutableList()
        maps.addAll(Database.waitingMaps)
        for (map in maps) {
            if (map.raceMap != this) continue
            map.stoped = true
            map.tlTask?.cancel()
            map.bonusesTask?.let { Bukkit.getScheduler().cancelTask(it) }
            map.bonuses.toList().forEach {
                it.first.remove()
            }
            for (playerUUID in map.players.toList()) {
                val offlinePlayer = +playerUUID
                map.removePlayer(playerUUID)
                offlinePlayer.player?.sendMessagePr(mainConfig.gameStop)
            }
            Database.availableRaceMaps[this] = HashSet()
        }
        Database.startedRaceMaps.removeIf { it.raceMap == this }
        Database.waitingMaps.removeIf { it.raceMap == this }
    }
}

fun StartedRaceMap.stop() {
    raceMap.stop()
}

fun RaceMap.disable() {
    stop()
    Database.availableRaceMaps.remove(this)?.let {
        for (playerUUID in it) {
            (+playerUUID).player?.sendMessagePr(mainConfig.mapDisabling)
        }
        status = MapStatus.DISABLED
        update()
    }
}

fun RaceMap.activate() {
    synchronized(SDBoat.instance) {
        if (!Database.availableRaceMaps.contains(this)) Database.availableRaceMaps[this] = HashSet()
        status = MapStatus.AVAILABLE
        update()
    }
}

fun RaceMap.addPlayerToQueue(playerUUID: UUID): Boolean {
    val offlinePlayer = +playerUUID
    synchronized(SDBoat.instance) {
        try {
            if (Database.startedRaceMaps.find { it.players.contains(playerUUID) } != null) {
                offlinePlayer.player?.sendMessagePr("<red>Вы уже участвуете в гонке</red>")
                return true
            }
            if (Database.availableRaceMaps.values.find { it.contains(playerUUID) } != null) {
                offlinePlayer.player?.sendMessagePr("<red>Вы уже в очереди</red>")
                return true
            }
            if (Database.availableRaceMaps[this]!!.size >= positionsList.size) return false
            Database.availableRaceMaps[this]!!.add(playerUUID)
            offlinePlayer.player?.teleport(mainConfig.lobby)
            prepareStart()
            offlinePlayer.player?.sendMessagePr(mainConfig.queueEnter)
            return true
        } catch (_: Exception) {
            try {
                Database.availableRaceMaps[this]!!.remove(playerUUID)
            } catch (_: Exception) {
            }
            return false
        }
    }
}

fun RaceMap.stopPreparing(players: Iterable<UUID>) {
    synchronized(SDBoat.instance) {
        for (playerUUID in players) {
            (+playerUUID).player?.sendMessagePr(mainConfig.mapDisabling)
        }
        Database.availableRaceMaps[this] = HashSet()
    }
}

fun RaceMap.prepareStart(): Boolean {
    synchronized(SDBoat.instance) {
        stop()
        val players = Database.availableRaceMaps[this] ?: return false
        if (players.isEmpty() || players.size < positionsList.size * (mainConfig.minPercentForStart / 100.0)) return false
        if (positionsList.size < players.size) {
            stopPreparing(players)
            return false
        }
        Bukkit.getScheduler().runTaskLater(SDBoat.instance, Runnable {
            try {
                if (Database.startedRaceMaps.find { it.raceMap == this } != null) return@Runnable
                val players1 = Database.availableRaceMaps[this] ?: return@Runnable
                if (players1.isEmpty()) return@Runnable
                if (positionsList.size < players1.size || players1.size < positionsList.size * (mainConfig.minPercentForStart / 100.0)) {
                    return@Runnable
                }
                start(players.toList())
            } catch (_: Exception) {

            }
        }, mainConfig.waitToStart * 20L)
        return true
    }
}

fun RaceMap.start(players: List<UUID>) {
    synchronized(SDBoat.instance) {
        val positions = positionsList
        Database.availableRaceMaps.remove(this)
        val startedRaceMap = StartedRaceMap(this, players.toMutableList(), 0)
        for ((i, playerUUID) in players.withIndex()) {
            val offlinePlayer = +playerUUID
            offlinePlayer.player?.sendMessagePr(mainConfig.gameStart)
            val loc = Location(
                null,
                positions[i].x,
                positions[i].y,
                positions[i].z,
                positions[i].yaw,
                positions[i].pitch
            )
            startedRaceMap.placePlayerInBoat(playerUUID, loc, false)
            offlinePlayer.player?.activePotionEffects?.toList()?.forEach { offlinePlayer.player?.removePotionEffect(it.type) }
            startedRaceMap.checkpoints[playerUUID] = loc
        }
        startedRaceMap.bonusesTask()
        Database.waitingMaps.add(startedRaceMap)
        players.forEach {
            (+it).player?.let { player ->
                (adventure.sender(player)).showTitle(
                    Title.title(
                        TextFormatter.format("<aqua>До начала</aqua>"),
                        TextFormatter.format("<red>3</red>")
                    )
                )
            }
        }
        Bukkit.getScheduler().runTaskLater(SDBoat.instance, Runnable {
            players.forEach {
                (+it).player?.let { player ->
                    (adventure.sender(player)).showTitle(
                        Title.title(
                            TextFormatter.format("<aqua>До начала</aqua>"),
                            TextFormatter.format("<gold>2</gold>")
                        )
                    )
                }
            }
        }, 15)
        Bukkit.getScheduler().runTaskLater(SDBoat.instance, Runnable {
            players.forEach {
                (+it).player?.let { player ->
                    (adventure.sender(player)).showTitle(
                        Title.title(
                            TextFormatter.format("<aqua>До начала</aqua>"),
                            TextFormatter.format("<green>1</green>")
                        )
                    )
                }
            }
        }, 30)
        Bukkit.getScheduler().runTaskLater(SDBoat.instance, Runnable {
            Database.waitingMaps.toList().forEach { map ->
                if (map.raceMap == startedRaceMap.raceMap) {
                    if (map.stoped) {
                        Database.waitingMaps.remove(map)
                        return@Runnable
                    }
                    Database.waitingMaps.remove(map)
                    map.startTime = System.currentTimeMillis()
                    map.tlTask = Bukkit.getScheduler().runTaskLater(SDBoat.instance, Runnable {
                        for (map1 in Database.startedRaceMaps.toList()) {
                            if (map1.raceMap != map.raceMap) continue
                            map1.players.toList().forEach {
                                map1.playerLose(it)
                            }
                            map1.stop()
                        }
                    }, mainConfig.maxRaceTime * 20L)
                    Database.startedRaceMaps.add(map)
                    map.players.toList().forEach {
                        if (!(+it).isOnline) map.removePlayer(it)
                        map.kill(it)
                        (+it).player?.let { player ->
                            (adventure.sender(player)).showTitle(
                                Title.title(
                                    TextFormatter.format("<green><bold>СТАРТ!</bold></green>"),
                                    TextFormatter.format("")
                                )
                            )
                        }
                    }
                }
            }
        }, 45)
    }
}

fun StartedRaceMap.bonusesTask() {
    bonusesTask?.let { Bukkit.getScheduler().cancelTask(it) }
    bonusesTask = Bukkit.getScheduler().scheduleSyncRepeatingTask(
        SDBoat.instance,
        {
            if (raceMap.bonuses.isEmpty()) return@scheduleSyncRepeatingTask
            val random = ThreadLocalRandom.current()
            val loc = +raceMap.bonuses.random()
            val world = Bukkit.getWorld(raceMap.world)!!
            loc.world = world
            when (random.nextInt(100)) {
                in 0..60 -> {
                    val entity = world.spawnEntity(loc, EntityType.ARMOR_STAND) as ArmorStand
                    entity.isVisible = false
                    entity.equipment!!.setItemInMainHand(Bonus.SPEED.item)
                    bonuses.add(entity to Bonus.SPEED)
                }

                in 60..90 -> {
                    val entity = world.spawnEntity(loc, EntityType.ARMOR_STAND) as ArmorStand
                    entity.isVisible = false
                    entity.equipment!!.setItemInMainHand(Bonus.BOMB.item)
                    bonuses.add(entity to Bonus.BOMB)
                }

                in 90..100 -> {
                    val entity = world.spawnEntity(loc, EntityType.ARMOR_STAND) as ArmorStand
                    entity.isVisible = false
                    entity.equipment!!.setItemInMainHand(Bonus.LAVA.item)
                    bonuses.add(entity to Bonus.LAVA)
                }
            }

        },
        0,
        mainConfig.nextBonusDelay.toLong()
    )
}

@Serializable
enum class MapStatus {
    DISABLED, AVAILABLE,
}

