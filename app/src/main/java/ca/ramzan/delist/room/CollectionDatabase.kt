package ca.ramzan.delist.room

import android.content.Context
import android.net.Uri
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import java.io.InputStream

const val DB_NAME = "collection_database"

@Database(
    entities = [
        Collection::class,
        Task::class,
    ],
    version = 1,
    exportSchema = true
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
                        DB_NAME
                    )
                        .setJournalMode(JournalMode.TRUNCATE)
                        .fallbackToDestructiveMigration()
                        .build()
                    INSTANCE = instance
                }
                return instance
            }
        }

        fun copyTo(context: Context, uri: Uri) {
            context.contentResolver.openOutputStream(uri)?.use { stream ->
                context.getDatabasePath(DB_NAME).inputStream().copyTo(stream)
            }
        }

        fun copyFrom(context: Context, uri: Uri) {
            context.contentResolver.openInputStream(uri)?.use { stream ->
                if (!isSQLite3File(stream)) return
            }
            context.contentResolver.openInputStream(uri)?.use { stream ->
                if (!isSchemaValid(context, stream)) return
            }

            INSTANCE?.close()
            INSTANCE = null
            val dbFile = context.getDatabasePath(DB_NAME)
            dbFile.delete()
            context.contentResolver.openInputStream(uri)?.use { stream ->
                stream.copyTo(dbFile.outputStream())
            }
        }

        private fun isSQLite3File(fis: InputStream): Boolean {
            val sqliteHeader = "SQLite format 3".toByteArray()
            val buffer = ByteArray(sqliteHeader.size)

            val count = fis.read(buffer)
            if (count < sqliteHeader.size) return false

            return buffer.contentEquals(sqliteHeader)
        }

        private fun isSchemaValid(context: Context, fis: InputStream): Boolean {
            // TODO
            return true
        }
    }
}