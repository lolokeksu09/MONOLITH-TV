package dev.monolith.tv.catalog

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable

data class TvApp(
    val component: ComponentName,
    val packageName: String,
    val title: String,
    val originalIcon: Drawable
)

/**
 * Discovers only explicitly launchable activities; no broad package permission.
 * The original Drawable comes from the installed application's metadata.
 */
class AppCatalog(context: Context) {
    private val packageManager: PackageManager = context.packageManager
    private val ownPackage = context.packageName

    @Suppress("DEPRECATION")
    fun list(): List<TvApp> {
        val result = linkedMapOf<String, TvApp>()
        for (category in listOf(Intent.CATEGORY_LEANBACK_LAUNCHER, Intent.CATEGORY_LAUNCHER)) {
            val intent = Intent(Intent.ACTION_MAIN).addCategory(category)
            val matches = try {
                packageManager.queryIntentActivities(intent, 0)
            } catch (_: SecurityException) {
                emptyList()
            }

            for (match in matches) {
                val info = match.activityInfo ?: continue
                val pkg = info.packageName
                // A parallel AMOLED review should not list another MONOLITH preview as an app.
                if (pkg == ownPackage || pkg == "dev.monolith.tv.preview" ||
                    pkg == "dev.monolith.tv.review" ||
                    pkg == "dev.monolith.tv.cinema" || result.containsKey(pkg)) continue
                try {
                    result[pkg] = TvApp(
                        component = ComponentName(pkg, info.name),
                        packageName = pkg,
                        title = match.loadLabel(packageManager).toString()
                            .ifBlank { pkg },
                        originalIcon = match.loadIcon(packageManager)
                            ?: packageManager.defaultActivityIcon
                    )
                } catch (_: RuntimeException) {
                    // Package may be removed or disabled while it is enumerated.
                }
            }
        }
        return result.values.toList()
    }
}

