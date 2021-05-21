package ca.ramzan.delist.common

import android.app.Application
import android.content.SharedPreferences
import android.view.inputmethod.InputMethodManager
import androidx.preference.PreferenceManager
import ca.ramzan.delist.room.CollectionDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
class AppModule {

    @Provides
    @Singleton
    fun db(app: Application) = CollectionDatabase.getInstance(app)

    @Provides
    @Singleton
    fun dao(db: CollectionDatabase) = db.collectionDatabaseDao

    @Provides
    @Singleton
    fun imm(app: Application): InputMethodManager =
        app.getSystemService(InputMethodManager::class.java)

    @Provides
    @Singleton
    fun prefs(app: Application): SharedPreferences =
        PreferenceManager.getDefaultSharedPreferences(app.applicationContext)
}