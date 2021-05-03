package ca.ramzan.delist.room

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface CollectionDatabaseDao {

    // region list----------------------------------------------------------------------------------
    @Update
    fun updateItem(item: Item)

    @Query("SELECT * FROM item_table WHERE item_table.id = :id")
    fun getItem(id: Long): Item

    @Query(
        """
        SELECT collection_table.id, name, color, content AS item
        FROM collection_table
        LEFT JOIN item_table
        ON collection_table.id = item_table.collectionId
        ORDER BY name COLLATE NOCASE ASC
    """
    )
    fun getCollectionDisplays(): Flow<List<CollectionDisplay>>

    @Transaction
    fun completeTask(collectionId: Long) {
        val collection = getCollection(collectionId)
        val oldItem = getItem(collection.currentItemId ?: return)
        val newItemId = when (collection.type) {
            CollectionType.STACK -> getTopOfStack(collection.id)
            CollectionType.QUEUE -> getFrontOfQueue(collection.id)
            CollectionType.RANDOMIZER -> getRandomItem(collection.id)
        }
        updateItem(oldItem.copy(completed = true))
        updateCollection(collection.copy(currentItemId = newItemId))
    }

    @Insert
    fun createCollection(collection: Collection)

    @Update
    fun updateCollection(collection: Collection)

    @Delete
    fun deleteCollection(collection: Collection)

    @Query("SELECT * FROM collection_table WHERE collection_table.id = :id")
    fun getCollection(id: Long): Collection

    @Query(
        """
            SELECT MIN(id)
            FROM item_table 
            WHERE item_table.collectionId = :collectionId
            AND NOT completed
        """
    )
    fun getFrontOfQueue(collectionId: Long): Long?

    @Query(
        """
            SELECT MAX(id)
            FROM item_table 
            WHERE item_table.collectionId = :collectionId
            AND NOT completed
        """
    )
    fun getTopOfStack(collectionId: Long): Long?

    @Query(
        """
            SELECT id 
            FROM item_table
            WHERE item_table.collectionId = :collectionId
            AND NOT completed
    """
    )
    fun getIncompleteItems(collectionId: Long): List<Long>

    fun getRandomItem(collectionId: Long): Long? {
        return getIncompleteItems(collectionId).randomOrNull()
    }

    fun changeCollectionType(collection: Collection, newType: CollectionType) {
        val newItemId = when (newType) {
            CollectionType.STACK -> getTopOfStack(collection.id)
            CollectionType.QUEUE -> getFrontOfQueue(collection.id)
            CollectionType.RANDOMIZER -> getRandomItem(collection.id)
        }
        updateCollection(collection.copy(type = newType, currentItemId = newItemId))
    }
    // endregion list-------------------------------------------------------------------------------

    // region detail--------------------------------------------------------------------------------
    @Insert
    fun createItem(item: Item)

    @Query("DELETE FROM item_table WHERE item_table.collectionId = :collectionId AND completed")
    fun deleteCompletedItems(collectionId: Long)
    // endregion detail-----------------------------------------------------------------------------
}