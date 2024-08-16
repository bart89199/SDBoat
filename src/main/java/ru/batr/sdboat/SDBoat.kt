package ru.batr.sdboat

import database.Database
import org.bukkit.plugin.java.JavaPlugin
import ru.batr.sdboat.config.MainConfig

class SDBoat : JavaPlugin() {

    companion object {
        lateinit var instance: SDBoat
            private set
        lateinit var mainConfig: MainConfig
            private set
    }
    override fun onEnable() {
        instance = this
        logger.info("Enabling SDBoat")
    //   ConfigurationSerialization.registerClass(::class.java)
        mainConfig = MainConfig()
     //   server.pluginManager.registerEvents((), this)
        Database.connect()
    }

    override fun onDisable() {
        logger.info("Disabling SDBoat")
    }
}