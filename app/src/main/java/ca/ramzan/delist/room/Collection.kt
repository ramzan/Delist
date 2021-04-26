package ca.ramzan.delist.room

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class CollectionType {
    STACK, QUEUE, RANDOM
}

@Entity(tableName = "collection_table")
data class Collection(
    val type: CollectionType,

    val currentItem: String?,

    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
)