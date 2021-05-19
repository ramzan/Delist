package ca.ramzan.delist.room

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey

@Entity(
    tableName = "task_table",
    foreignKeys = [ForeignKey(
        entity = Collection::class,
        parentColumns = ["id"],
        childColumns = ["collectionId"],
        onDelete = ForeignKey.CASCADE
    )]
)
data class Task(
    @ColumnInfo(index = true)
    val collectionId: Long,

    val content: String,

    val timeCompleted: Long? = null,

    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
)

class CompletedTaskDisplay(
    val id: Long,
    val content: String
)
