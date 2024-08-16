package database

import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.transaction
import racemap.RaceMap
import ru.batr.sdboat.SDBoat
import java.io.File

object Database {
    lateinit var raceMaps: Map<String, RaceMap>
        private set

    fun connect() {
        Database.connect("jdbc:sqlite:${SDBoat.instance.dataFolder}${File.separator}database.db")
        transaction {
            SchemaUtils.create(RaceMaps)
        }
        update()
    }

    fun update() {
        transaction {
            val map = HashMap<String, RaceMap>()
            RaceMaps.selectAll().forEach { map[it[RaceMaps.name]] = it[RaceMaps.map] }
            raceMaps = map
        }
    }

    fun addRaceMap(name: String, map: RaceMap) {
        transaction {
            RaceMaps.insert {
                it[RaceMaps.name] = name
                it[RaceMaps.map] = map
            }
        }
        update()
    }

    fun removeRaceMap(name: String) {
        transaction {
            RaceMaps.deleteWhere { RaceMaps.name eq stringLiteral(name) }
        }
        update()
    }

    fun updateRaceMap(name: String, newMap: RaceMap, newName: String = name) {
        transaction {
            RaceMaps.update({RaceMaps.name eq stringLiteral(name) }) {
                it[RaceMaps.name] = newName
                it[RaceMaps.map] = newMap
            }
        }
        update()
    }
}