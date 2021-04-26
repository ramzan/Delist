package ca.ramzan.delist.room

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        Collection::class,
        Item::class,
    ],
    version = 1,
    exportSchema = false
)
abstract class CollectionDatabase : RoomDatabase() {

    abstract val collectionDatabaseDao: CollectionDatabaseDao

    companion object {
        @Volatile
        private var INSTANCE: CollectionDatabase? = null

        fun getInstance(context: Context): CollectionDatabase {
            synchronized(this) {
                var instance = INSTANCE
                if (instance == null) {
                    instance = Room.databaseBuilder(
                        context.applicationContext,
                        CollectionDatabase::class.java,
                        "exercise_database"
                    )
                        .fallbackToDestructiveMigration()
                        .build()
                    INSTANCE = instance
                }
                return instance
            }
        }
    }
}