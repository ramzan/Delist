package ca.ramzan.delist.room

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class CollectionType {
    QUEUE, STACK, RANDOMIZER
}

enum class CollectionColor {
    PLAIN,
    RED,
    ORANGE,
    YELLOW,
    GREEN,
    TEAL,
    BLUE,
    DARK_BLUE,
    PURPLE,
    PINK,
    BROWN,
    GREY,
}

@Entity(tableName = "collection_table")
data class Collection(
    val type: CollectionType,

    val name: String,

    val color: CollectionColor,

    val currentTaskId: Long?,

    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
)

class CollectionDisplayData(
    val id: Long,
    val name: String,
    val color: CollectionColor,
    val task: String?
)