package ca.ramzan.delist.room

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface CollectionDatabaseDao {
    @Insert
    fun insertItem(item: Item)

    @Delete
    fun deleteItem(item: Item)

    @Insert
    fun insert(collection: Collection)

    @Update
    fun updateCollection(collection: Collection)

    @Delete
    fun deleteCollection(collection: Collection)

    @Transaction
    fun completeTask(collection: Collection) {
        val newItem = when (collection.type) {
            CollectionType.STACK -> getTopOfStack(collection.id)
            CollectionType.QUEUE -> getFrontOfQueue(collection.id)
            CollectionType.RANDOM -> getRandomItem(collection.id)
        }
        updateCollection(collection.copy(currentItem = newItem?.content))
        if (newItem != null) deleteItem(newItem)
    }

    @Query(
        """
            SELECT * FROM collection_table
        """
    )
    fun getCollections(): Flow<List<Collection>>

    @Query(
        """
            SELECT collectionId, content, MIN(id) as id 
            FROM item_table 
            WHERE item_table.collectionId = :collectionId
            LIMIT 1
        """
    )
    fun getFrontOfQueue(collectionId: Long): Item?

    @Query(
        """
            SELECT collectionId, content, MAX(id) as id 
            FROM item_table 
            WHERE item_table.collectionId = :collectionId
            LIMIT 1        
        """
    )
    fun getTopOfStack(collectionId: Long): Item?

    @Query(
        """
            SELECT * FROM item_table
            WHERE item_table.collectionId = :collectionId
            ORDER BY RANDOM()
            LIMIT 1
        """
    )
    fun getRandomItem(collectionId: Long): Item?
}