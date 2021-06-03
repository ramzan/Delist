package ca.ramzan.delist.room

import android.content.Context
import android.database.DatabaseUtils
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteDatabaseCorruptException
import android.net.Uri
import android.util.Log
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import java.io.File
import java.io.InputStream


const val DB_NAME = "collection_database"
const val DB_VERSION = 1
private const val TAG = "DelistImporter"
private const val IDENTITY_HASH = "e0c62562fa5d21eb4ba21f0bf61b1798"

@Database(
    entities = [
        Collection::class,
        Task::class,
    ],
    version = DB_VERSION,
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
            Log.d(TAG, "Starting import")
            context.contentResolver.openInputStream(uri)?.use { stream ->
                if (!isSQLite3File(stream)) {
                    Log.d(TAG, "Invalid header")
                    Log.d(TAG, "Import cancelled")
                    return
                }
            }
            context.contentResolver.openInputStream(uri)?.use { stream ->
                if (!isSchemaValid(context, stream)) {
                    Log.d(TAG, "Import cancelled")
                    return
                }
            }
            INSTANCE?.close()
            INSTANCE = null
            val dbFile = context.getDatabasePath(DB_NAME)
            dbFile.delete()
            context.contentResolver.openInputStream(uri)?.use { stream ->
                stream.copyTo(dbFile.outputStream())
            }
            Log.d(TAG, "Import complete")
        }

        private fun isSQLite3File(fis: InputStream): Boolean {
            val sqliteHeader = "SQLite format 3".toByteArray()
            val buffer = ByteArray(sqliteHeader.size)
            return (fis.read(buffer) >= sqliteHeader.size && buffer.contentEquals(sqliteHeader))
        }

        private fun isSchemaValid(context: Context, fis: InputStream): Boolean {
            val temp = File.createTempFile("import", "db", context.cacheDir)
            fis.copyTo(temp.outputStream())
            val db: SQLiteDatabase

            try {
                db = SQLiteDatabase.openDatabase(
                    temp.absolutePath,
                    null,
                    SQLiteDatabase.OPEN_READONLY
                )
            } catch (e: SQLiteDatabaseCorruptException) {
                Log.d(TAG, "Database corrupt")
                temp.delete()
                return false
            }

            val valid = run {
                if (db.version > DB_VERSION) {
                    return@run false
                }

                // Check for 6 tables/indices
                DatabaseUtils.queryNumEntries(db, "SQLITE_MASTER").let { numEntries ->
                    if (numEntries != 6L) {
                        Log.d(
                            TAG,
                            "Incorrect number of tables/indices: Expected: 6, Got: $numEntries"
                        )
                        return@run false
                    }
                }

                // Check correct table/index names
                var cursor = db.query(
                    "SQLITE_MASTER",
                    emptyArray<String>(),
                    "((name = ? OR name = ? OR name = ? OR name = ? OR name = ?) " +
                            "AND type = 'table') OR (name = ? AND type = 'index')",
                    arrayOf(
                        "android_metadata",
                        "collection_table",
                        "room_master_table",
                        "sqlite_sequence",
                        "task_table",
                        "index_task_table_collectionId"
                    ),
                    null,
                    null,
                    null
                )

                if (cursor.count != 6) {
                    Log.d(TAG, "Incorrect db structure")
                    cursor.close()
                    return@run false
                }
                cursor.close()

                // Check room identity hash
                cursor = db.query(
                    "room_master_table",
                    arrayOf("identity_hash"),
                    null,
                    null,
                    null,
                    null,
                    null
                )
                cursor.moveToFirst()
                if (cursor.count != 1 || cursor.getString(0) != IDENTITY_HASH) {
                    Log.d(TAG, "Bad identity hash")
                    cursor.close()
                    return@run false
                }
                cursor.close()
                true
            }
            db.close()
            temp.delete()
            return valid
        }
    }
}