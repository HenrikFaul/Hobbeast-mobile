package com.hobbeast.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.lifecycle.lifecycleScope
import com.hobbeast.app.data.local.UserPreferencesStore
import com.hobbeast.app.data.remote.SupabaseDataSource
import com.hobbeast.app.ui.navigation.HobbeastMainScaffold
import com.hobbeast.app.ui.navigation.Screen
import com.hobbeast.app.ui.theme.HobbeastTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject lateinit var supabase: SupabaseDataSource
    @Inject lateinit var prefs: UserPreferencesStore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Determine start destination synchronously (fast DataStore read)
        val startDestination = runBlocking {
            when {
                !supabase.isAuthenticated()    -> Screen.Login.route
                !prefs.onboardingDone.first()  -> Screen.Onboarding.route
                else                           -> Screen.Discovery.route
            }
        }

        // Handle deep links from push notifications
        val deepLinkPath = intent.getStringExtra("navigate_to")

        setContent {
            HobbeastTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    HobbeastMainScaffold(startDestination = deepLinkPath ?: startDestination)
                }
            }
        }
    }
}
