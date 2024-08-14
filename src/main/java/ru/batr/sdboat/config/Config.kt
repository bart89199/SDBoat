package ru.batr.shinedungeons.configs

import com.google.common.base.Charsets
import org.bukkit.configuration.file.YamlConfiguration
import ru.batr.shinedungeons.ShineDungeons
import java.io.File
import java.io.IOException
import java.io.InputStreamReader
import java.util.logging.Level

abstract class Config(val fileName: String, val path: String) {
    lateinit var config: YamlConfiguration
        protected set
    lateinit var file: File
        protected set
    val fullPath: String
            get() = if (path != "") "$path${File.separator}$fileName.yml" else "$fileName.yml"

    init {
        load()
    }

    fun load() {
        file = File("${ShineDungeons.instance.dataFolder}${File.separator}$fullPath")
        if (!file.exists()) {
            ShineDungeons.instance.saveResource(fullPath, false)
        }
        reload()
    }

    fun reload() {
        config = YamlConfiguration.loadConfiguration(file)

        config.setDefaults(
            YamlConfiguration.loadConfiguration(
                InputStreamReader(
                    ShineDungeons.instance.getResource(fullPath) ?: return, Charsets.UTF_8
                )
            )
        )
    }

    fun save() {
        try {
            config.save(file)
        } catch (e: IOException) {
            e.printStackTrace()
            ShineDungeons.instance.logger.log(
                Level.SEVERE,
                "Problems with save $fileName.yml please check updates or contact with author"
            )
        }
    }
}