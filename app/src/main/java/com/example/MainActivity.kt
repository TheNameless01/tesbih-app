package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import com.example.core.datastore.UserPreferences
import com.example.navigation.MusabbihApp
import com.example.ui.theme.MusabbihTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val app = application as MusabbihApplication

        setContent {
            val userPrefs by app.userPreferencesRepository.userPreferencesFlow
                .collectAsState(initial = UserPreferences())

            MusabbihTheme(themePreference = userPrefs.themeMode) {
                Surface(modifier = Modifier.fillMaxSize()) {
                    MusabbihApp(app = app)
                }
            }
        }
    }
}
