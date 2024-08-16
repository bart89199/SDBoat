package ru.batr.sdboat.config

import com.google.common.base.Charsets
import org.bukkit.configuration.file.YamlConfiguration
import ru.batr.sdboat.SDBoat
import java.io.File
import java.io.IOException
import java.io.InputStreamReader
import java.util.logging.Level

abstract class Config(val fileName: String) {
    lateinit var config: YamlConfiguration
        protected set
    lateinit var file: File
        protected set

    init {
        load()
    }

    fun load() {
        file = File("${SDBoat.instance.dataFolder}${File.separator}$fileName")
        if (!file.exists()) {
            SDBoat.instance.saveResource(fileName, false)
        }
        reload()
    }

    fun reload() {
        config = YamlConfiguration.loadConfiguration(file)

        config.setDefaults(
            YamlConfiguration.loadConfiguration(
                InputStreamReader(
                    SDBoat.instance.getResource(fileName) ?: return, Charsets.UTF_8
                )
            )
        )
    }

    fun save() {
        try {
            config.save(file)
        } catch (e: IOException) {
            e.printStackTrace()
            SDBoat.instance.logger.log(
                Level.SEVERE,
                "Problems with save $fileName please check updates or contact with author"
            )
        }
    }
}