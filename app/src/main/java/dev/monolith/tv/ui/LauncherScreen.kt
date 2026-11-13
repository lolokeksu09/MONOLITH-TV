package dev.monolith.tv.ui

import android.view.KeyEvent
import android.widget.ImageView
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.tv.material3.Text
import dev.monolith.tv.R
import dev.monolith.tv.catalog.AppOrder
import dev.monolith.tv.catalog.CatalogFilter
import dev.monolith.tv.catalog.LauncherOptions
import dev.monolith.tv.catalog.TvApp
import kotlinx.coroutines.delay
import java.time.LocalTime
import java.time.format.DateTimeFormatter

private val Background = Color(0xFF0B0D10)
private val TileBackground = Color(0xFF171A1E)
private val TileFocused = Color(0xFF262D33)
private val Primary = Color(0xFFF3F4F5)
private val Secondary = Color(0xFFABB1B9)
private val Border = Color(0xFF393D43)

@Composable
fun LauncherScreen(
    apps: List<TvApp>,
    favoritePackages: Set<String>,
    loading: Boolean,
    catalogError: Boolean,
    options: LauncherOptions,
    onLaunch: (TvApp) -> Unit,
    onToggleFavorite: (String) -> Unit,
    onOptionsChange: (LauncherOptions) -> Unit,
    onOpenAndroidSettings: () -> Unit
) {
    var onlyFavorites by rememberSaveable { mutableStateOf(false) }
    var settingsVisible by rememberSaveable { mutableStateOf(false) }
    var lastFocusedKey by rememberSaveable { mutableStateOf<String?>(null) }
    var pendingFocusKey by remember { mutableStateOf<String?>(null) }

    val ordered = remember(apps, favoritePackages, onlyFavorites, options.favoritesFirst) {
        AppOrder.visible(
            apps, favoritePackages, { it.packageName }, { it.title },
            if (onlyFavorites) CatalogFilter.FAVORITES else CatalogFilter.ALL,
            options.favoritesFirst
        )
    }
    val knownFavoriteCount = remember(apps, favoritePackages) {
        apps.count { it.packageName in favoritePackages }
    }

    val gridState = rememberLazyGridState()
    val appRequesters = remember { mutableMapOf<String, FocusRequester>() }
    val allRequester = remember { FocusRequester() }
    val favoritesRequester = remember { FocusRequester() }
    val settingsRequester = remember { FocusRequester() }

    val clock by produceState(initialValue = formattedTime()) {
        while (true) {
            value = formattedTime()
            delay(15_000)
        }
    }

    BackHandler(enabled = settingsVisible) {
        settingsVisible = false
    }

    // Restore the same component after favorite reordering and after closing
    // settings. A moved or removed tile is scrolled into view before focus.
    LaunchedEffect(loading, settingsVisible, onlyFavorites, ordered) {
        if (loading || settingsVisible) return@LaunchedEffect

        if (ordered.isEmpty()) {
            withFrameNanos { }
            if (onlyFavorites) favoritesRequester.requestFocus()
            else settingsRequester.requestFocus()
            pendingFocusKey = null
            return@LaunchedEffect
        }

        val key = (pendingFocusKey ?: lastFocusedKey)
            ?.takeIf { candidate ->
                ordered.any { it.component.flattenToString() == candidate }
            } ?: ordered.first().component.flattenToString()
        val index = ordered.indexOfFirst { it.component.flattenToString() == key }

        gridState.scrollToItem(index)
        withFrameNanos { }
        withFrameNanos { }
        if (gridState.layoutInfo.visibleItemsInfo.any { it.key == key }) {
            appRequesters[key]?.requestFocus()
        }
        pendingFocusKey = null
    }

    if (settingsVisible) {
        SettingsScreen(
            options = options,
            onOptionsChange = onOptionsChange,
            onClose = { settingsVisible = false },
            onOpenAndroidSettings = onOpenAndroidSettings
        )
    } else {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Background)
                .padding(horizontal = 46.dp, vertical = 30.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        stringResource(R.string.app_name),
                        color = Primary,
                        fontSize = 29.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(Modifier.height(5.dp))
                    Text(
                        stringResource(R.string.apps_meta, apps.size, knownFavoriteCount),
                        color = Secondary,
                        fontSize = 13.sp
                    )
                    Text(stringResource(R.string.remote_hint), color = Secondary, fontSize = 13.sp)
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(clock, color = Primary, fontSize = 23.sp)
                    Spacer(Modifier.width(18.dp))
                    ActionTile(
                        stringResource(R.string.all_apps),
                        onClick = { onlyFavorites = false },
                        selected = !onlyFavorites,
                        requester = allRequester
                    )
                    Spacer(Modifier.width(8.dp))
                    ActionTile(
                        stringResource(R.string.favorites_tab),
                        onClick = { onlyFavorites = true },
                        selected = onlyFavorites,
                        requester = favoritesRequester
                    )
                    Spacer(Modifier.width(8.dp))
                    ActionTile(
                        stringResource(R.string.settings),
                        onClick = { settingsVisible = true },
                        requester = settingsRequester
                    )
                }
            }

            Spacer(Modifier.height(20.dp))
            when {
                loading -> Text(
                    stringResource(R.string.loading),
                    color = Secondary, fontSize = 18.sp
                )
                ordered.isEmpty() -> {
                    Text(
                        stringResource(
                            if (catalogError) R.string.catalog_error
                            else if (onlyFavorites) R.string.no_favorites
                            else R.string.no_apps
                        ),
                        color = Primary,
                        fontSize = 18.sp
                    )
                }
                else -> LazyVerticalGrid(
                    columns = GridCells.Adaptive(
                        minSize = if (options.compactGrid) 154.dp else 170.dp
                    ),
                    state = gridState,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(ordered, key = { it.component.flattenToString() }) { app ->
                        val key = app.component.flattenToString()
                        val requester = appRequesters.getOrPut(key) { FocusRequester() }
                        AppTile(
                            app = app,
                            requester = requester,
                            compact = options.compactGrid,
                            isFavorite = app.packageName in favoritePackages,
                            onFocused = { lastFocusedKey = key },
                            onLaunch = { onLaunch(app) },
                            onToggleFavorite = {
                                pendingFocusKey = key
                                onToggleFavorite(app.packageName)
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AppTile(
    app: TvApp,
    requester: FocusRequester,
    compact: Boolean,
    isFavorite: Boolean,
    onFocused: () -> Unit,
    onLaunch: () -> Unit,
    onToggleFavorite: () -> Unit
) {
    var focused by remember { mutableStateOf(false) }
    val shape = RoundedCornerShape(10.dp)
    val favoriteLabel = stringResource(R.string.favorite)

    Column(
        modifier = Modifier
            .height(if (compact) 96.dp else 118.dp)
            .focusRequester(requester)
            .onFocusChanged {
                focused = it.isFocused
                if (it.isFocused) onFocused()
            }
            .onPreviewKeyEvent { event ->
                if (event.type == KeyEventType.KeyUp &&
                    event.nativeKeyEvent.keyCode == KeyEvent.KEYCODE_MENU
                ) {
                    onToggleFavorite()
                    true
                } else false
            }
            .border(
                width = if (focused) 2.dp else 1.dp,
                color = if (focused) Primary else Border,
                shape = shape
            )
            .background(if (focused) TileFocused else TileBackground, shape)
            .semantics {
                contentDescription = app.title + if (isFavorite) " " + favoriteLabel else ""
            }
            .clickable(onClick = onLaunch)
            .padding(horizontal = 12.dp, vertical = if (compact) 7.dp else 9.dp),
        horizontalAlignment = Alignment.Start,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        // Draw installed app-provided artwork unchanged. No tint, clip or mask.
        AndroidView(
            factory = { viewContext ->
                ImageView(viewContext).apply {
                    scaleType = ImageView.ScaleType.FIT_CENTER
                    adjustViewBounds = true
                }
            },
            update = { view ->
                view.setImageDrawable(
                    app.originalIcon.constantState?.newDrawable()?.mutate() ?: app.originalIcon
                )
            },
            modifier = Modifier.size(if (compact) 42.dp else 52.dp)
        )
        Column {
            Text(
                app.title,
                color = Primary,
                fontSize = if (compact) 14.sp else 16.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                if (isFavorite) favoriteLabel else " ",
                color = Secondary,
                fontSize = 11.sp,
                maxLines = 1
            )
        }
    }
}

@Composable
private fun SettingsScreen(
    options: LauncherOptions,
    onOptionsChange: (LauncherOptions) -> Unit,
    onClose: () -> Unit,
    onOpenAndroidSettings: () -> Unit
) {
    val doneRequester = remember { FocusRequester() }
    LaunchedEffect(Unit) {
        withFrameNanos { }
        doneRequester.requestFocus()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Background)
            .padding(horizontal = 46.dp, vertical = 32.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    stringResource(R.string.launcher_settings),
                    color = Primary, fontSize = 29.sp, fontWeight = FontWeight.SemiBold
                )
                Text(
                    stringResource(R.string.settings_subtitle),
                    color = Secondary, fontSize = 14.sp
                )
            }
            ActionTile(
                stringResource(R.string.done),
                onClick = onClose,
                requester = doneRequester
            )
        }
        Spacer(Modifier.height(26.dp))
        PreferenceTile(
            title = stringResource(R.string.compact_grid),
            description = stringResource(R.string.compact_grid_description),
            value = stringResource(
                if (options.compactGrid) R.string.enabled else R.string.disabled
            ),
            onClick = {
                onOptionsChange(options.copy(compactGrid = !options.compactGrid))
            }
        )
        Spacer(Modifier.height(10.dp))
        PreferenceTile(
            title = stringResource(R.string.favorites_first),
            description = stringResource(R.string.favorites_first_description),
            value = stringResource(
                if (options.favoritesFirst) R.string.enabled else R.string.disabled
            ),
            onClick = {
                onOptionsChange(options.copy(favoritesFirst = !options.favoritesFirst))
            }
        )
        Spacer(Modifier.height(10.dp))
        PreferenceTile(
            title = stringResource(R.string.android_settings),
            description = stringResource(R.string.android_settings_description),
            value = "",
            onClick = onOpenAndroidSettings
        )
        Spacer(Modifier.height(20.dp))
        Text(
            stringResource(R.string.settings_saved),
            color = Secondary,
            fontSize = 13.sp
        )
    }
}

@Composable
private fun PreferenceTile(
    title: String,
    description: String,
    value: String,
    onClick: () -> Unit
) {
    var focused by remember { mutableStateOf(false) }
    val shape = RoundedCornerShape(10.dp)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(86.dp)
            .onFocusChanged { focused = it.isFocused }
            .border(
                if (focused) 2.dp else 1.dp,
                if (focused) Primary else Border,
                shape
            )
            .background(if (focused) TileFocused else TileBackground, shape)
            .clickable(onClick = onClick)
            .padding(horizontal = 20.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, color = Primary, fontSize = 18.sp, fontWeight = FontWeight.Medium)
            Spacer(Modifier.height(4.dp))
            Text(description, color = Secondary, fontSize = 13.sp)
        }
        if (value.isNotEmpty()) {
            Spacer(Modifier.width(20.dp))
            Text(value, color = if (focused) Primary else Secondary, fontSize = 14.sp)
        }
    }
}

@Composable
private fun ActionTile(
    title: String,
    onClick: () -> Unit,
    selected: Boolean = false,
    requester: FocusRequester? = null
) {
    var focused by remember { mutableStateOf(false) }
    val shape = RoundedCornerShape(10.dp)
    Box(
        modifier = Modifier
            .then(if (requester == null) Modifier else Modifier.focusRequester(requester))
            .onFocusChanged { focused = it.isFocused }
            .border(
                if (focused) 2.dp else 1.dp,
                if (focused) Primary else if (selected) Secondary else Border,
                shape
            )
            .background(
                if (focused || selected) TileFocused else TileBackground,
                shape
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 15.dp, vertical = 11.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(title, color = if (selected || focused) Primary else Secondary, fontSize = 15.sp)
    }
}

private val TimeFormat = DateTimeFormatter.ofPattern("HH:mm")
private fun formattedTime(): String = LocalTime.now().format(TimeFormat)
