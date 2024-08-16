package racemap

import kotlinx.serialization.Serializable

@Serializable
data class RaceMap(
    val world: String,
    val start: Pair<Int, Int>,
    val end: Pair<Int, Int>,
)