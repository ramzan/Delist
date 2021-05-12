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
        SELECT collection_table.id, name, color, content AS task
        FROM collection_table
        LEFT JOIN task_table
        ON collection_table.currentTaskId = task_table.id
        ORDER BY name COLLATE NOCASE ASC
    """
    )
    fun getCollectionDisplays(): Flow<List<CollectionDisplayData>>

    @Query(
        """
        SELECT collection_table.id, name, color, content AS task
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
        updateTask(oldTask.copy(completed = true))
        val newTaskId = when (collection.type) {
            CollectionType.STACK -> getTopOfStack(collection.id)
            CollectionType.QUEUE -> getFrontOfQueue(collection.id)
            CollectionType.RANDOMIZER -> getRandomTask(collection.id)
        }
        updateCollection(collection.copy(currentTaskId = newTaskId))
    }

    @Insert
    fun createCollection(collection: Collection)

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
            AND NOT completed
        """
    )
    fun getFrontOfQueue(collectionId: Long): Long?

    @Query(
        """
            SELECT MAX(id)
            FROM task_table 
            WHERE task_table.collectionId = :collectionId
            AND NOT completed
        """
    )
    fun getTopOfStack(collectionId: Long): Long?

    @Query(
        """
            SELECT id 
            FROM task_table
            WHERE task_table.collectionId = :collectionId
            AND NOT completed
    """
    )
    fun getIncompleteTasks(collectionId: Long): List<Long>

    @Query(
        """
            SELECT id, content
            FROM task_table
            WHERE task_table.collectionId = :collectionId
            AND completed
    """
    )
    fun getCompletedTasks(collectionId: Long): Flow<List<CompletedTaskDisplay>>


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

    @Query("DELETE FROM task_table WHERE task_table.collectionId = :collectionId AND completed")
    fun deleteCompletedTasks(collectionId: Long)
    // endregion detail-----------------------------------------------------------------------------
}