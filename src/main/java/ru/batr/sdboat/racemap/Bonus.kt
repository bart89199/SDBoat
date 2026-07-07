package ru.batr.sdboat.racemap

import TextFormatter
import kotlinx.serialization.Serializable
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.NamespacedKey
import org.bukkit.Sound
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.meta.PotionMeta
import org.bukkit.persistence.PersistentDataType
import org.bukkit.potion.PotionEffect
import org.bukkit.potion.PotionEffectType
import org.bukkit.potion.PotionType
import ru.batr.sdboat.SDBoat
import ru.batr.sdboat.SDBoat.Companion.mainConfig

val speedItem by lazy {
    val item = ItemStack(Material.POTION)
    val meta = item.itemMeta as PotionMeta
    meta.basePotionType = PotionType.SWIFTNESS
    meta.displayName(TextFormatter.format("<aqua>Ускоритель</aqua>"))
    meta.persistentDataContainer.set(
        NamespacedKey(SDBoat.instance, "bonus"),
        PersistentDataType.STRING,
        "speed"
    )
    item.itemMeta = meta
    item
}
val bombItem by lazy {
    val item = ItemStack(Material.TNT)
    val meta = item.itemMeta!!
    meta.displayName(TextFormatter.format("<gold>Бомба</gold>"))
    meta.persistentDataContainer.set(
        NamespacedKey(SDBoat.instance, "bonus"),
        PersistentDataType.STRING,
        "bomb"
    )
    item.itemMeta = meta
    item
}
val lavaItem by lazy {
    val item = ItemStack(Material.LAVA_BUCKET)
    val meta = item.itemMeta!!
    meta.displayName(TextFormatter.format("<red>ПОЛ ЭТО ЛАВА</red>"))
    meta.persistentDataContainer.set(
        NamespacedKey(SDBoat.instance, "bonus"),
        PersistentDataType.STRING,
        "lava"
    )
    item.itemMeta = meta
    item
}
val breadItem by lazy {
    val item = ItemStack(Material.BREAD)
    val meta = item.itemMeta!!
    meta.persistentDataContainer.set(
        NamespacedKey(SDBoat.instance, "bonus"),
        PersistentDataType.STRING,
        "bread"
    )
    item.itemMeta = meta
    item
}
val goldenAppleItem by lazy {
    val item = ItemStack(Material.GOLDEN_APPLE)
    val meta = item.itemMeta!!
    meta.persistentDataContainer.set(
        NamespacedKey(SDBoat.instance, "bonus"),
        PersistentDataType.STRING,
        "goldenapple"
    )
    item.itemMeta = meta
    item
}
val fireResistantPotionItem by lazy {
    val item = ItemStack(Material.POTION)
    val meta = item.itemMeta as PotionMeta
    meta.basePotionType = PotionType.FIRE_RESISTANCE
    meta.persistentDataContainer.set(
        NamespacedKey(SDBoat.instance, "bonus"),
        PersistentDataType.STRING,
        "fireresistant"
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
            it.addPotionEffect(PotionEffect(PotionEffectType.SPEED, 3 * 20, 3))
        },
        speedItem
    ),
    BREAD(
        {
            it.addPotionEffect(PotionEffect(PotionEffectType.SATURATION, 3 * 20, 1))
        },
        breadItem,
    ),
    GOLDEN_APPLE(
        {
            it.addPotionEffect(PotionEffect(PotionEffectType.SATURATION, 3 * 20, 1))
            it.addPotionEffect(
                PotionEffect(
                    PotionEffectType.REGENERATION,
                    3 * 20,
                    3
                )
            )
        },
        goldenAppleItem,
    ),
    FIRE_RESISTANT(
        {
            it.addPotionEffect(
                PotionEffect(
                    PotionEffectType.FIRE_RESISTANCE,
                    10 * 20,
                    1
                )
            )
        },
        fireResistantPotionItem,
    ),
    BOMB(
        { player ->
            player.getNearbyEntities(100.0, 100.0, 100.0).filterIsInstance<Player>()
                .firstOrNull { players.contains(it.uniqueId) }
                ?.let {
                    it.world.spawnParticle(org.bukkit.Particle.EXPLOSION, it.location, 30)
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
                Bukkit.getScheduler().runTaskLater(SDBoat.instance, Runnable {
                    world.getBlockAt(+coordinates).type = block
                }, mainConfig.lavaBonusTime.toLong())
            }
            for (x in start.x.toInt()..end.x.toInt()) {
                for (z in start.z.toInt()..end.z.toInt()) {
                    for (y in loc.y.toInt() - 5..loc.y.toInt() + 5) {
                        val block = world.getBlockAt(x, y, z)
                        if (!block.isEmpty && block.type != Material.LAVA) {
                            addRestoreTask(Coordinates(x.toDouble(), y.toDouble(), z.toDouble()), block.type)
                            block.type = Material.LAVA
                        }
                    }
                }
            }
        },
        lavaItem
    ),
}

fun Bonus.check(item: ItemStack): Boolean {
    if (this.item.type != item.type) return false
    val meta = item.itemMeta
    val orMeta = this.item.itemMeta
    val data = meta.persistentDataContainer
    val orData = orMeta.persistentDataContainer
    return data.get(
        NamespacedKey(SDBoat.instance, "bonus"),
        PersistentDataType.STRING
    ) == orData.get(
        NamespacedKey(SDBoat.instance, "bonus"),
        PersistentDataType.STRING
    )
}