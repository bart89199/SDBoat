package ru.batr.sdboat.racemap

import TextFormatter
import kotlinx.serialization.Serializable
import net.kyori.adventure.platform.bukkit.BukkitComponentSerializer
import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.Sound
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.meta.PotionMeta
import org.bukkit.potion.PotionData
import org.bukkit.potion.PotionType
import ru.batr.sdboat.SDBoat
import ru.batr.sdboat.SDBoat.Companion.adventure
import ru.batr.sdboat.SDBoat.Companion.mainConfig

val speedItem by lazy {
    val item = ItemStack(Material.POTION)
    val meta = item.itemMeta as PotionMeta
    meta.basePotionData = PotionData(PotionType.SPEED)
    meta.setDisplayName(BukkitComponentSerializer.legacy().serialize(TextFormatter.format("<aqua>Ускоритель</aqua>")))
    meta.persistentDataContainer.set(
        org.bukkit.NamespacedKey(SDBoat.instance, "bonus"),
        org.bukkit.persistence.PersistentDataType.STRING,
        "speed"
    )
    item.itemMeta = meta
    item
}
val bombItem by lazy {
    val item = ItemStack(Material.TNT)
    val meta = item.itemMeta!!
    meta.setDisplayName(BukkitComponentSerializer.legacy().serialize(TextFormatter.format("<gold>Бомба</gold>")))
    meta.persistentDataContainer.set(
        org.bukkit.NamespacedKey(SDBoat.instance, "bonus"),
        org.bukkit.persistence.PersistentDataType.STRING,
        "bomb"
    )
    item.itemMeta = meta
    item
}
val lavaItem by lazy {
    val item = ItemStack(Material.LAVA_BUCKET)
    val meta = item.itemMeta!!
    meta.setDisplayName(BukkitComponentSerializer.legacy().serialize(TextFormatter.format("<red>ПОЛ ЭТО ЛАВА</red>")))
    meta.persistentDataContainer.set(
        org.bukkit.NamespacedKey(SDBoat.instance, "bonus"),
        org.bukkit.persistence.PersistentDataType.STRING,
        "lava"
    )
    item.itemMeta = meta
    item
}

@Serializable
enum class Bonus(
    val activate: StartedRaceMap.(player: Player) -> Unit,
    val item: ItemStack,
) {
    SPEED(
        {
            it.addPotionEffect(org.bukkit.potion.PotionEffect(org.bukkit.potion.PotionEffectType.SPEED, 3 * 20, 3))
        },
        speedItem
    ),
    BOMB(
        { player ->
            player.getNearbyEntities(100.0, 100.0, 100.0).filterIsInstance<Player>().firstOrNull { players.contains(it.uniqueId) }
                ?.let {
                    it.world.spawnParticle(org.bukkit.Particle.EXPLOSION_HUGE, it.location, 30)
                    it.playSound(it.location, Sound.ENTITY_GENERIC_EXPLODE, 20f, 0f)
                    Bukkit.getScheduler().runTaskLater(SDBoat.instance, java.lang.Runnable {
                        kill(it.uniqueId)
                    }, 10)
                }
        },
        bombItem
    ),
    LAVA(
        { player ->
            val loc = player.location
            val start: SimpleCoordinates
            val end: SimpleCoordinates
            val world = player.world
            when (player.location.yaw.toInt()) {
                in 0..45 -> {
                    start = SimpleCoordinates(loc.x - 3, loc.z - 6)
                    end = SimpleCoordinates(loc.x + 3, loc.z - 2)
                }
                in -45..0 -> {
                    start = SimpleCoordinates(loc.x - 3, loc.z - 6)
                    end = SimpleCoordinates(loc.x + 3, loc.z - 2)
                }
                in 45..120 -> {
                    start = SimpleCoordinates(loc.x + 2, loc.z - 3)
                    end = SimpleCoordinates(loc.x + 6, loc.z + 3)
                }
                in -120..-45 -> {
                    start = SimpleCoordinates(loc.x - 6, loc.z - 3)
                    end = SimpleCoordinates(loc.x - 2, loc.z + 3)
                }
                else -> {
                    start = SimpleCoordinates(loc.x - 3, loc.z + 2)
                    end = SimpleCoordinates(loc.x + 3, loc.z + 6)
                }
            }
            fun addRestoreTask(coordinates: Coordinates, block: Material) {
                Bukkit.getScheduler().runTaskLater(SDBoat.instance, java.lang.Runnable {
                    world.getBlockAt(+coordinates).type = block
                }, mainConfig.lavaBonusTime.toLong())
            }
            for (x in start.x.toInt()..end.x.toInt()) {
                for (z in start.z.toInt()..end.z.toInt()) {
                    for (y in loc.y.toInt() - 5..loc.y.toInt() + 5) {
                        val block = world.getBlockAt(x, y, z)
                        if (!block.isEmpty && block.type != Material.MAGMA_BLOCK) {
                            addRestoreTask(Coordinates(x.toDouble(), y.toDouble(), z.toDouble()), block.type)
                            block.type = Material.MAGMA_BLOCK
                        }
                    }
                }
            }
        },
        lavaItem
    ),
}

fun Bonus.check(item: ItemStack): Boolean {
    return this.item.type == item.type && item.itemMeta!!.persistentDataContainer.keys.containsAll(this.item.itemMeta!!.persistentDataContainer.keys)
}