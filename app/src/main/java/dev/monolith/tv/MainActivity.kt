package dev.monolith.tv

import android.content.ActivityNotFoundException
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.lifecycleScope
import androidx.tv.material3.MaterialTheme
import dev.monolith.tv.catalog.AppCatalog
import dev.monolith.tv.catalog.TvApp
import dev.monolith.tv.storage.FavoritesStore
import dev.monolith.tv.ui.LauncherScreen
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : ComponentActivity() {
    private lateinit var catalog: AppCatalog
    private lateinit var favoritesStore: FavoritesStore

    private val apps = mutableStateOf<List<TvApp>>(emptyList())
    private val loading = mutableStateOf(true)
    private val favorites = mutableStateOf<Set<String>>(emptySet())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        catalog = AppCatalog(this)
        favoritesStore = FavoritesStore(this)
        favorites.value = favoritesStore.load()

        setContent {
            MaterialTheme {
                LauncherScreen(
                    apps = apps.value,
                    favoritePackages = favorites.value,
                    loading = loading.value,
                    onLaunch = ::launchApp,
                    onToggleFavorite = { pkg -> favorites.value = favoritesStore.toggle(pkg) },
                    onOpenSettings = ::openSettings
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        loading.value = apps.value.isEmpty()
        lifecycleScope.launch {
            val found = withContext(Dispatchers.IO) { catalog.list() }
            apps.value = found
            loading.value = false
        }
    }

    private fun launchApp(app: TvApp) {
        try {
            startActivity(
                Intent(Intent.ACTION_MAIN)
                    .setComponent(app.component)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED)
            )
        } catch (_: ActivityNotFoundException) {
            Toast.makeText(this, R.string.app_unavailable, Toast.LENGTH_SHORT).show()
            refreshCatalog()
        } catch (_: SecurityException) {
            Toast.makeText(this, R.string.app_unavailable, Toast.LENGTH_SHORT).show()
            refreshCatalog()
        }
    }

    private fun refreshCatalog() {
        lifecycleScope.launch {
            apps.value = withContext(Dispatchers.IO) { catalog.list() }
        }
    }

    private fun openSettings() {
        try {
            startActivity(Intent(Settings.ACTION_SETTINGS))
        } catch (_: ActivityNotFoundException) {
            Toast.makeText(this, R.string.settings_unavailable, Toast.LENGTH_SHORT).show()
        }
    }
}

