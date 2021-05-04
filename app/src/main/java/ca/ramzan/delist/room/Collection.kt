package ca.ramzan.delist.room

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class CollectionType {
    QUEUE, STACK, RANDOMIZER
}

@Entity(tableName = "collection_table")
data class Collection(
    val type: CollectionType,

    val name: String,

    val color: String,

    val currentItemId: Long?,

    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
)

class CollectionDisplay(val id: Long, val name: String, val color: String, val item: String?)