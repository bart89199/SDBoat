package ru.batr.sdboat

import TextFormatter
import net.kyori.adventure.audience.Audience
import net.kyori.adventure.text.Component
import org.bukkit.command.CommandSender
import org.bukkit.plugin.java.JavaPlugin
import ru.batr.sdboat.command.Commands
import ru.batr.sdboat.config.MainConfig
import ru.batr.sdboat.database.Database

class SDBoat : JavaPlugin() {

    companion object {
        lateinit var instance: SDBoat
            private set
        lateinit var mainConfig: MainConfig
            private set

        fun Audience.sendMessagePr(component: Component) {
            sendMessage(mainConfig.prefix.append(Component.text(" ")).append(component))
        }

        fun Audience.sendMessagePr(message: String) {
            sendMessagePr(TextFormatter.format(message))
        }

        fun Audience.sendMessage(message: String) {
            sendMessage(TextFormatter.format(message))
        }

        fun CommandSender.sendMessage(component: Component, withPrefix: Boolean = false) {
            if (withPrefix) (this as Audience).sendMessagePr(component)
            else (this as Audience).sendMessage(component)
        }

        fun CommandSender.sendMessagePr(message: String) {
            (this as Audience).sendMessagePr(TextFormatter.format(message))
        }
    }

    override fun onEnable() {
        instance = this
        //   ConfigurationSerialization.registerClass(::class.java)
        mainConfig = MainConfig()
        //   server.pluginManager.registerEvents((), this)
        Database.connect()
        Commands.init()

        server.consoleSender.sendMessagePr("<green>Plugin Enabled!</green>")
    }

    override fun onDisable() {
        server.consoleSender.sendMessagePr("<aqua>Plugin disabled!</aqua>")
    }
}