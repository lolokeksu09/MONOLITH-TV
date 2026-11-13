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
import dev.monolith.tv.catalog.LauncherOptions
import dev.monolith.tv.catalog.TvApp
import dev.monolith.tv.storage.FavoritesStore
import dev.monolith.tv.storage.LauncherOptionsStore
import dev.monolith.tv.ui.LauncherScreen
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : ComponentActivity() {
    private lateinit var catalog: AppCatalog
    private lateinit var favoritesStore: FavoritesStore
    private lateinit var optionsStore: LauncherOptionsStore
    private var catalogJob: Job? = null

    private val apps = mutableStateOf<List<TvApp>>(emptyList())
    private val loading = mutableStateOf(true)
    private val catalogError = mutableStateOf(false)
    private val favorites = mutableStateOf<Set<String>>(emptySet())
    private val options = mutableStateOf(LauncherOptions())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        catalog = AppCatalog(this)
        favoritesStore = FavoritesStore(this)
        optionsStore = LauncherOptionsStore(this)
        favorites.value = favoritesStore.load()
        options.value = optionsStore.load()

        setContent {
            MaterialTheme {
                LauncherScreen(
                    apps = apps.value,
                    favoritePackages = favorites.value,
                    loading = loading.value,
                    catalogError = catalogError.value,
                    options = options.value,
                    onLaunch = ::launchApp,
                    onToggleFavorite = { pkg ->
                        favorites.value = favoritesStore.toggle(pkg)
                    },
                    onOptionsChange = { updated ->
                        optionsStore.save(updated)
                        options.value = updated
                    },
                    onOpenAndroidSettings = ::openAndroidSettings
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        refreshCatalog()
    }

    private fun refreshCatalog() {
        catalogJob?.cancel()
        loading.value = apps.value.isEmpty()
        catalogJob = lifecycleScope.launch {
            val result = try {
                withContext(Dispatchers.IO) { catalog.list() }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: RuntimeException) {
                null
            }

            if (result == null) {
                catalogError.value = true
            } else {
                apps.value = result
                catalogError.value = false
            }
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
            notifyUnavailable()
        } catch (_: SecurityException) {
            notifyUnavailable()
        }
    }

    private fun notifyUnavailable() {
        Toast.makeText(this, R.string.app_unavailable, Toast.LENGTH_SHORT).show()
        refreshCatalog()
    }

    private fun openAndroidSettings() {
        try {
            startActivity(Intent(Settings.ACTION_SETTINGS))
        } catch (_: ActivityNotFoundException) {
            Toast.makeText(this, R.string.settings_unavailable, Toast.LENGTH_SHORT).show()
        } catch (_: SecurityException) {
            Toast.makeText(this, R.string.settings_unavailable, Toast.LENGTH_SHORT).show()
        }
    }
}
