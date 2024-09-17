package ru.batr.sdboat.config

import TextFormatter
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder
import org.bukkit.Bukkit
import org.bukkit.Location
import ru.batr.sdboat.command.toInt
import ru.batr.sdboat.config.conteiner.Container

class MainConfig : Config("config.yml") {
    val prefix by Container.ConfigComponentContainer(
        this,
        "prefix",
        "<gray>[<gradient:blue:aqua><bold>SDBoat</bold></gradient>]</gray>",
    )
    var spawn by Container.ConfigContainer(
        this,
        "spawn",
        Location(Bukkit.getWorld("world"), 0.0, 0.0, 0.0),
        Container.toLocation
    )
    var lobby by Container.ConfigContainer(
        this,
        "lobby",
        Location(Bukkit.getWorld("world"), 0.0, 0.0, 0.0),
        Container.toLocation
    )
    val bonusRange by Container.ConfigContainer(
        this,
        "bonusRange",
        3,
        Container.toInt
    )
    val checkpointRange by Container.ConfigContainer(
        this,
        "checkpointRange",
        5,
        Container.toInt
    )
    val minPercentForStart by Container.ConfigContainer(
        this,
        "minPercentForStart",
        50.0,
        Container.toDouble
    )
    val gameStop by Container.ConfigComponentContainer(
        this,
        "gameStop",
        "<red>Игра была остановлена!</red>",
    )
    val mapDisabling by Container.ConfigComponentContainer(
        this,
        "mapDisabling",
        "<red>Карта была отключена! Вы больше не в очереди!</red>",
    )
    val gameStart by Container.ConfigComponentContainer(
        this,
        "gameStart",
        "<aqua>Игра начинается!</aqua>",
    )
    val queueEnter by Container.ConfigComponentContainer(
        this,
        "queueEnter",
        "<aqua>Вы вошли в очередь, ожидайте!</aqua>",
    )
    val mapLeave by Container.ConfigComponentContainer(
        this,
        "mapLeave",
        "<red>Вы вышли за пределы карты!</red>",
    )
    val queueLeave by Container.ConfigComponentContainer(
        this,
        "queueLeave",
        "<aqua>Вы покинули очередь</aqua>",
    )
    val gameLeave by Container.ConfigComponentContainer(
        this,
        "gameLeave",
        "<aqua>Вы покинули игру</aqua>",
    )
    val checkPointMessage by Container.ConfigComponentContainer(
        this,
        "checkPointMessage",
        "<aqua>Вы прошли чекпоинт!</aqua>",
    )
    fun finishMessage(place: Int, time: String) = Container.ConfigComponentContainer(
        this,
        "finishMessage",
        "gold>Поздравляю! Ваше место: <aqua><place></aqua>, время: <aqua><time></aqua>! </gold>",
        Placeholder.component("place", TextFormatter.format(place.toString())),
        Placeholder.component("time", TextFormatter.format(time)),
    ).value
    val maxRaceTime by Container.ConfigContainer(
        this,
        "maxRaceTime",
        300,
        Container.toInt
    )
    val winPlaces by Container.ConfigContainer(
        this,
        "winPlaces",
        1,
        Container.toInt
    )
    val waitToStart by Container.ConfigContainer(
        this,
        "waitToStart",
        10,
        Container.toInt
    )
    val nextBonusDelay by Container.ConfigContainer(
        this,
        "nextBonusDelay",
        80,
        Container.toInt
    )
    val lavaBonusTime by Container.ConfigContainer(
        this,
        "lavaBonusTime",
        80,
        Container.toInt
    )
    val topUpdateTime by Container.ConfigContainer(
        this,
        "topUpdateTime",
        80,
        Container.toInt
    )
}