package ca.ramzan.delist.common

import android.content.Context
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.view.WindowCompat
import ca.ramzan.delist.R
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        AppCompatDelegate.setDefaultNightMode(
            getPreferences(Context.MODE_PRIVATE).getInt(
                getString(R.string.theme),
                AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
            )
        )
        WindowCompat.setDecorFitsSystemWindows(window, false)
        setContentView(R.layout.activity_main)
    }
}