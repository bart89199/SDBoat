package ru.batr.shinedungeons

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.minimessage.MiniMessage
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver

object TextFormatter {

    val DEFAULT_SERIALIZER: MiniMessage = MiniMessage.miniMessage()

    val DEFAULT_PLACEHOLDERS: Array<TagResolver> = arrayOf()

    fun format(
        input: String,
        serializer: MiniMessage = DEFAULT_SERIALIZER,
        vararg placeholders: TagResolver = DEFAULT_PLACEHOLDERS
    ) = serializer.deserialize(input, *placeholders)

    fun format(
        input: Component,
        serializer: MiniMessage = DEFAULT_SERIALIZER,
    ) = serializer.serialize(input)

    fun format(
        input: List<String>,
        serializer: MiniMessage = DEFAULT_SERIALIZER,
        vararg placeholders: TagResolver = DEFAULT_PLACEHOLDERS
    ): List<Component> {
        val output = ArrayList<Component>();

        for (i in input) {
            output.add(serializer.deserialize(i, *placeholders))
        }
        return output
    }

    fun format(
        input: List<Component>,
        serializer: MiniMessage = DEFAULT_SERIALIZER,
    ): List<String> {
        val output = ArrayList<String>();

        for (i in input) {
            output.add(serializer.serialize(i))
        }
        return output
    }

}