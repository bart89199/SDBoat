package ru.batr.shinedungeons.configs.containers

import com.google.common.graph.Graph
import org.bukkit.Location
import org.bukkit.OfflinePlayer
import org.bukkit.configuration.ConfigurationSection
import org.bukkit.inventory.ItemStack
import ru.batr.shinedungeons.configs.Config
import ru.batr.shinedungeons.configs.containers.Container.Converter
import ru.batr.shinedungeons.configs.containers.Container.Saver

interface Container<T> {

    var value: T

    fun interface Converter<T> {
        fun toT(value: Any?): T?
    }

    fun interface Saver<T> {
        fun save(value: T, path: String, config: Config)
    }

    open class DefaultContainer<T>(
        val config: Config,
        val path: String,
        val default: T,
        val converter: Converter<T>,
        val saver: Saver<T> = defaultSaver()
    ) : Container<T> {
        override var value: T
            get() {
                val value = config.config.get(path)
                return converter.toT(value) ?: default
            }
            set(value) = saver.save(value, path, config)
    }

    open class ListContainer<T> (
        val config: Config,
        val path: String,
        val default: List<T>,
        val converter: Converter<T>,
        val saver: Saver<List<T>> = defaultSaver(),
    ) : Container<List<T>> {
        override var value: List<T>
            get() {
                val value = config.config.getList(path) ?: return default
                val output = ArrayList<T>()
                for (i in value) {
                    output.add(converter.toT(i) ?: continue)
                }
                return output
            }
            set(value) = saver.save(value, path, config)
        fun add(value: T) {
            val list = this.value.toMutableList()
            list.add(value)
            
            this.value = list
        }
        fun set(index: Int, value: T) {
            val list = this.value.toMutableList()
            list[index] = value
            this.value = list
        }
        fun remove(value: T): Boolean {
            val list = this.value.toMutableList()
            val res = list.remove(value)
            this.value = list
            return res
        }
        fun removeAt(index: Int): T {
            val list = this.value.toMutableList()
            val res = list.removeAt(index)
            this.value = list
            return res
        }

    }

    open class MapContainer<T> (
        val config: Config,
        val path: String,
        val default: Map<String, T>,
        val converter: Converter<T>,
        val saver: Saver<Map<String, T>> = saveMap(),
    ) : Container<Map<String, T>> {
        override var value: Map<String, T>
            get() {
                val input = config.config.get(path) ?: return default
                return if (input is ConfigurationSection) {
                    val keys = input.getKeys(false)
                    val output = HashMap<String, T>()
                    for (key in keys) {
                        output[key] = converter.toT(input.get(key) ?: continue) ?: continue
                    }
                    output
                } else if (input is Map<*, *>) {
                    val output = HashMap<String, T>()
                    for ((v, k) in input) {
                        output[v.toString()] = converter.toT(k ?: continue) ?: continue
                    }
                    output
                } else default
            }
            set(value) = saver.save(value, path, config)
        fun set(key: String, value: T) {
            val map = this.value.toMutableMap()
            map[key] = value
            this.value = map
        }
        fun remove(key: String): T? {
            val map = this.value.toMutableMap()
            val res = map.remove(key)
            this.value = map
            return res
        }
        fun remove(key: String, value: T): Boolean {
            val map = this.value.toMutableMap()
            val res = map.remove(key, value)
            this.value = map
            return res
        }
    }

    companion object {
        fun <T> defaultSaver() = Saver<T> { value, path1, config1 ->
            config1.config.set(path1, value)
            config1.save()
        }

        val toInt = Converter { input: Any? -> if (input is Number) input.toInt() else null }
        val toByte = Converter { input: Any? -> if (input is Number) input.toByte() else null }
        val toShort = Converter { input: Any? -> if (input is Number) input.toShort() else null }
        val toDouble = Converter { input: Any? -> if (input is Number) input.toDouble() else null }
        val toFloat = Converter { input: Any? -> if (input is Number) input.toFloat() else null }
        val toLong = Converter { input: Any? -> if (input is Number) input.toLong() else null }
        val toBoolean = Converter { input: Any? -> if (input is Boolean) input else null }
        val toString = Converter { input: Any? -> input?.toString() }
        val toItemStack = Converter { input: Any? -> if (input is ItemStack) input else null }
        val toLocation = Converter { input: Any? -> if (input is Location) input else null }
        val toOfflinePlayer = Converter { input: Any? -> if (input is OfflinePlayer) input else null }

        fun <T> toList(converter: Converter<T>) = Converter { input: Any? ->
            if (input is List<*>) {
                val output = ArrayList<T>()
                for (i in input) {
                    output.add(converter.toT(i ?: continue) ?: continue)
                }
                output
            } else null
        }

        fun <T> toMap(converter: Converter<T>) = Converter<Map<String, T>> { input: Any? ->
            if (input is ConfigurationSection) {
                val keys = input.getKeys(false)
                val output = HashMap<String, T>()
                for (key in keys) {
                    output[key] = converter.toT(input.get(key) ?: continue) ?: continue
                }
                output
            } else if (input is Map<*, *>) {
                val output = HashMap<String, T>()
                for ((v, k) in input) {
                    output[v.toString()] = converter.toT(k ?: continue) ?: continue
                }
                output
            } else null
        }

        fun <T> saveMap(saver: Saver<T> = defaultSaver()) =
            Saver { map: Map<String, T>, path: String, config: Config ->
                config.config.set(path, null)
                for ((k, v) in map) {
                    saver.save(v, "$path.$k", config)
                }
                config.save()
            }
    }
}
