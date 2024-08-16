package ru.batr.sdboat.config

import TextFormatter
import ru.batr.sdboat.config.conteiner.Container

class MainConfig : Config("config.yml") {
    val prefix by Container.ConfigContainer(
        this,
        "prefix",
        TextFormatter.format("<gray>[<gradient:blue:aqua><bold>SDBoat</bold></gradient>]</gray>"),
        Container.toComponent
    )
}