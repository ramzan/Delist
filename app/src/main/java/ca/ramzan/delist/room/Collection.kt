package ca.ramzan.delist.room

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class CollectionType {
    QUEUE, STACK, RANDOMIZER
}

enum class CollectionColor(s: String) {
    PLAIN("PLAIN"),
    RED("RED"),
    ORANGE("ORANGE"),
    YELLOW("YELLOW"),
    GREEN("GREEN"),
    TEAL("TEAL"),
    BLUE("BLUE"),
    DARK_BLUE("DARK_BLUE"),
    PURPLE("PURPLE"),
    PINK("PINK"),
    BROWN("BROWN"),
    GREY("GREY"),
}

@Entity(tableName = "collection_table")
data class Collection(
    val type: CollectionType,

    val name: String,

    val color: CollectionColor,

    val currentItemId: Long?,

    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
)

class CollectionDisplayData(
    val id: Long,
    val name: String,
    val color: CollectionColor,
    val item: String?
)