package ca.ramzan.delist.room

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface CollectionDatabaseDao {

    // region list----------------------------------------------------------------------------------
    @Update
    fun updateTask(task: Task)

    @Query("SELECT * FROM task_table WHERE task_table.id = :id")
    fun getTask(id: Long): Task

    @Query(
        """
        SELECT collection_table.id, name, color, archived, content AS task
        FROM collection_table
        LEFT JOIN task_table
        ON collection_table.currentTaskId = task_table.id
        ORDER BY displayOrder
    """
    )
    fun getCollectionDisplaysManual(): Flow<List<CollectionDisplayData>>

    @Query(
        """
        SELECT collection_table.id, name, color, archived, content AS task
        FROM collection_table
        LEFT JOIN task_table
        ON collection_table.currentTaskId = task_table.id
        ORDER BY name COLLATE NOCASE ASC
    """
    )
    fun getCollectionDisplaysAsc(): Flow<List<CollectionDisplayData>>

    @Query(
        """
        SELECT collection_table.id, name, color, archived, content AS task
        FROM collection_table
        LEFT JOIN task_table
        ON collection_table.currentTaskId = task_table.id
        ORDER BY name COLLATE NOCASE DESC
    """
    )
    fun getCollectionDisplaysDesc(): Flow<List<CollectionDisplayData>>

    @Query(
        """
        SELECT collection_table.id, name, color, archived, content AS task
        FROM collection_table
        LEFT JOIN task_table
        ON collection_table.currentTaskId = task_table.id
        WHERE collection_table.id = :collectionId
    """
    )
    fun getCollectionDisplay(collectionId: Long): Flow<CollectionDisplayData?>

    @Transaction
    fun completeTask(collectionId: Long) {
        val collection = getCollection(collectionId)
        val oldTask = getTask(collection.currentTaskId ?: return)
        updateTask(oldTask.copy(timeCompleted = System.currentTimeMillis()))
        val newTaskId = when (collection.type) {
            CollectionType.STACK -> getTopOfStack(collection.id)
            CollectionType.QUEUE -> getFrontOfQueue(collection.id)
            CollectionType.RANDOMIZER -> getRandomTask(collection.id)
        }
        updateCollection(collection.copy(currentTaskId = newTaskId))
    }

    @Transaction
    fun undoCompleteTask(collectionId: Long) {
        val collection = getCollection(collectionId)
        val lastCompletedTask = getLastCompletedTask(collectionId) ?: return
        updateTask(lastCompletedTask.copy(timeCompleted = null))
        updateCollection(collection.copy(currentTaskId = lastCompletedTask.id))
    }

    @Query(
        """
            SELECT collectionId, content, MAX(timeCompleted) as timeCompleted, id
            FROM task_table 
            WHERE task_table.collectionId = :collectionId
            AND timeCompleted IS NOT NULL
            LIMIT 1
        """
    )
    fun getLastCompletedTask(collectionId: Long): Task?

    @Insert
    fun insertCollection(collection: Collection): Long

    fun createCollection(name: String, type: CollectionType, color: CollectionColor): Long {
        return insertCollection(Collection(type, name, color, getLastCollectionOrder() + 1))
    }

    @Query(
        """
            SELECT id
            FROM collection_table
            ORDER BY displayOrder
        """
    )
    fun getCollectionOrdering(): List<Long>

    @Transaction
    fun updateCollectionsOrder(movedId: Long, behindId: Long) {
        val sortedIds = getCollectionOrdering().toMutableList()
        val fromPos = sortedIds.indexOf(movedId)
        val toPos = sortedIds.indexOf(behindId)
        var i = fromPos

        if (fromPos < toPos) while (i < toPos) sortedIds[i] = sortedIds[++i]
        else while (i > toPos) sortedIds[i] = sortedIds[--i]
        sortedIds[toPos] = movedId

        var j = 1L
        sortedIds.forEach { id -> saveCollection(getCollection(id).copy(displayOrder = j++)) }
    }

    @Query("SELECT MAX(displayOrder) from collection_table")
    fun getLastCollectionOrder(): Long

    @Update
    fun saveCollection(collection: Collection)

    @Delete
    fun deleteCollection(collection: Collection)

    @Query("SELECT * FROM collection_table WHERE collection_table.id = :id")
    fun getCollection(id: Long): Collection

    @Query(
        """
            SELECT MIN(id)
            FROM task_table 
            WHERE task_table.collectionId = :collectionId
            AND timeCompleted IS NULL
        """
    )
    fun getFrontOfQueue(collectionId: Long): Long?

    @Query(
        """
            SELECT MAX(id)
            FROM task_table 
            WHERE task_table.collectionId = :collectionId
            AND timeCompleted IS NULL
        """
    )
    fun getTopOfStack(collectionId: Long): Long?

    @Query(
        """
            SELECT id 
            FROM task_table
            WHERE task_table.collectionId = :collectionId
            AND timeCompleted IS NULL
    """
    )
    fun getIncompleteTasks(collectionId: Long): List<Long>

    @Query(
        """
            SELECT id, content, timeCompleted AS completed
            FROM task_table
            WHERE task_table.collectionId = :collectionId
            ORDER BY timeCompleted
    """
    )
    fun getTasks(collectionId: Long): Flow<List<TaskDisplay>>


    fun getRandomTask(collectionId: Long): Long? {
        return getIncompleteTasks(collectionId).randomOrNull()
    }

    // endregion list-------------------------------------------------------------------------------

    // region editor--------------------------------------------------------------------------------

    @Transaction
    fun updateCollection(collection: Collection, typeChanged: Boolean = false) {
        if (typeChanged) {
            val newTaskId = when (collection.type) {
                CollectionType.STACK -> getTopOfStack(collection.id)
                CollectionType.QUEUE -> getFrontOfQueue(collection.id)
                CollectionType.RANDOMIZER -> getRandomTask(collection.id)
            }
            saveCollection(collection.copy(currentTaskId = newTaskId))
        } else saveCollection(collection)
    }

    // endregion editor-----------------------------------------------------------------------------

    // region detail--------------------------------------------------------------------------------
    @Insert
    fun createTasks(tasks: List<Task>): List<Long>

    @Transaction
    fun addTasks(tasks: List<Task>) {
        if (tasks.isEmpty()) return
        val taskIds = createTasks(tasks)
        val collection = getCollection(tasks.first().collectionId)
        if (collection.type == CollectionType.STACK) {
            updateCollection(collection.copy(currentTaskId = taskIds.last()))
        } else if (collection.currentTaskId == null) {
            if (collection.type == CollectionType.RANDOMIZER) {
                updateCollection(collection.copy(currentTaskId = taskIds.random()))
            } else {
                updateCollection(collection.copy(currentTaskId = taskIds.first()))
            }
        }
    }

    @Query("DELETE FROM task_table WHERE task_table.collectionId = :collectionId AND timeCompleted IS NOT NULL")
    fun deleteCompletedTasks(collectionId: Long)

    fun archiveCollection(collectionId: Long, isArchived: Boolean) {
        saveCollection(getCollection(collectionId).copy(archived = isArchived))
    }

    @Query("SELECT * FROM task_table WHERE collectionId = :collectionId")
    suspend fun getAllTasks(collectionId: Long): List<Task>

    @Query("SELECT id, name FROM collection_table")
    suspend fun getAllCollections(): List<CollectionExport>
    // endregion detail-----------------------------------------------------------------------------
}