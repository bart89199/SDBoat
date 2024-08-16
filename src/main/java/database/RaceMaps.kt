package database

import kotlinx.serialization.json.Json
import racemap.RaceMap
import org.jetbrains.exposed.dao.id.IntIdTable
import org.jetbrains.exposed.sql.json.json

const val MAX_VARCHAR_LENGTH = 128
val jsonSerializer = Json { prettyPrint = true }

object RaceMaps : IntIdTable("raceMaps") {
    val name = varchar("name", MAX_VARCHAR_LENGTH).uniqueIndex()
    val map = json<RaceMap>("racemap", jsonSerializer)
}