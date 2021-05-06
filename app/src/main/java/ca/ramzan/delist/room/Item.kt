package ca.ramzan.delist.room

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey

@Entity(
    tableName = "item_table",
    foreignKeys = [ForeignKey(
        entity = Collection::class,
        parentColumns = ["id"],
        childColumns = ["collectionId"],
        onDelete = ForeignKey.CASCADE
    )]
)
data class Item(
    @ColumnInfo(index = true)
    val collectionId: Long,

    val content: String,

    val completed: Boolean = false,

    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
)

class CompletedItemDisplay(
    val id: Long,
    val content: String
)
