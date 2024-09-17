package ru.batr.sdboat.database

import org.jetbrains.exposed.exceptions.ExposedSQLException
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.transaction
import ru.batr.sdboat.SDBoat
import ru.batr.sdboat.racemap.MapStatus
import ru.batr.sdboat.racemap.RaceMap
import ru.batr.sdboat.racemap.StartedRaceMap
import java.io.File
import java.util.UUID

object Database {
    val raceMaps: MutableList<RaceMap> = ArrayList()
    val availableRaceMaps: MutableMap<RaceMap, MutableSet<UUID>> = HashMap()
    val startedRaceMaps: MutableList<StartedRaceMap> = ArrayList()
    val executingPlayers: MutableList<UUID> = ArrayList()
    val waitingMaps: MutableList<StartedRaceMap> = ArrayList()

    fun connect() {
        Database.connect("jdbc:sqlite:${SDBoat.instance.dataFolder}${File.separator}database.db")
        transaction {
            SchemaUtils.create(RaceMaps)
        }
        update()
        raceMaps.forEach {
            if (it.status == MapStatus.AVAILABLE) {
                availableRaceMaps[it] = HashSet()
            }
        }
    }

    fun update() {
        synchronized(SDBoat.instance) {
            transaction {
                raceMaps.clear()
                RaceMaps.selectAll().forEach { raceMaps.add(it[RaceMaps.map].copy(name = it[RaceMaps.name])) }
            }
        }
    }

    fun addRaceMap(raceMap: RaceMap): Boolean {
        synchronized(SDBoat.instance) {
            var status = true
            transaction {
                try {
                    RaceMaps.insert {
                        it[name] = raceMap.name
                        it[map] = raceMap
                    }
                } catch (e: ExposedSQLException) {
                    status = false
                }
            }
            update()
            if (status) {
                raceMaps.add(raceMap)
                if (raceMap.status == MapStatus.AVAILABLE) {
                    availableRaceMaps[raceMap] = HashSet()
                }
            }
            return status
        }
    }

    fun removeRaceMap(name: String) {
        synchronized(SDBoat.instance) {
            transaction {
                RaceMaps.deleteWhere { RaceMaps.name eq stringLiteral(name) }
            }
            update()
        }
    }

    fun updateRaceMap(name: String, newMap: RaceMap): Boolean {
        synchronized(SDBoat.instance) {
            var status = true
            transaction {
                try {
                    RaceMaps.update({ RaceMaps.name eq stringLiteral(name) }) {
                        it[RaceMaps.name] = newMap.name
                        it[RaceMaps.map] = newMap
                    }
                } catch (e: ExposedSQLException) {
                    status = false
                }
            }
            update()
            if (status) {
                raceMaps.removeIf { it.name == name }
                raceMaps.add(newMap)
                for ((key, value) in availableRaceMaps) {
                    if (key.name == name) {
                        availableRaceMaps.remove(key)
                        availableRaceMaps[newMap] = value
                        break
                    }
                }
                for (raceMap in startedRaceMaps) {
                    if (raceMap.raceMap.name == name) {
                        raceMap.raceMap = newMap
                    }
                }
            }
            return status
        }
    }
}