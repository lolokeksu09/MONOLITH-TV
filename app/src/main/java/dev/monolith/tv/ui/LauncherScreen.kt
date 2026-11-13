package dev.monolith.tv.ui

import android.view.KeyEvent
import android.widget.ImageView
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
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

// MONOLITH TV · editorial gallery. Quiet surfaces, one unmistakable focus state.
// The installed application's own Drawable is never cropped, tinted or replaced.
private val Canvas = Color(0xFF090B0E)
private val Rail = Color(0xFF14171C)
private val Surface = Color(0xFF1A1E24)
private val SurfaceFocused = Color(0xFF303741)
private val Primary = Color(0xFFF4F5F7)
private val Secondary = Color(0xFFB0B9C4)
private val Accent = Color(0xFFE1EAF4)
private val Hairline = Color(0xFF303741)

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
    val featured = remember(ordered, lastFocusedKey) {
        ordered.firstOrNull { it.component.flattenToString() == lastFocusedKey }
            ?: ordered.firstOrNull()
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

    // Keep the selected application after MENU reorders the grid and after
    // returning from settings. A removed item is never focused.
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

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(Canvas)
    ) {
        val narrow = maxWidth < 800.dp
        val short = maxHeight < 510.dp
        val railWidth = if (narrow) 176.dp else 214.dp
        val sideGap = if (narrow) 16.dp else 24.dp
        val edge = if (narrow) 20.dp else 28.dp
        val tileHeight = if (options.compactGrid) {
            if (short) 104.dp else 116.dp
        } else {
            if (short) 116.dp else 128.dp
        }

        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = edge, vertical = if (short) 18.dp else 24.dp),
            horizontalArrangement = Arrangement.spacedBy(sideGap)
        ) {
            BrandRail(
                modifier = Modifier
                    .width(railWidth)
                    .fillMaxHeight(),
                clock = clock,
                meta = if (settingsVisible) {
                    stringResource(R.string.settings_subtitle)
                } else {
                    stringResource(R.string.apps_meta, apps.size, knownFavoriteCount)
                },
                app = if (settingsVisible) null else featured,
                favorite = featured?.packageName in favoritePackages,
                title = if (settingsVisible) {
                    stringResource(R.string.launcher_settings)
                } else {
                    stringResource(
                        if (onlyFavorites) R.string.favorites_tab else R.string.apps_heading
                    )
                },
                footer = stringResource(
                    if (settingsVisible) R.string.settings_saved else R.string.remote_hint
                ),
                compact = narrow,
                short = short
            )

            if (settingsVisible) {
                SettingsScreen(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight(),
                    options = options,
                    short = short,
                    onOptionsChange = onOptionsChange,
                    onClose = { settingsVisible = false },
                    onOpenAndroidSettings = onOpenAndroidSettings
                )
            } else {
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = stringResource(
                                if (onlyFavorites) R.string.favorites_tab else R.string.apps_heading
                            ),
                            color = Primary,
                            fontSize = if (narrow) 22.sp else 27.sp,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )
                        Spacer(Modifier.width(10.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            NavAction(
                                title = stringResource(R.string.all_apps),
                                onClick = { onlyFavorites = false },
                                selected = !onlyFavorites,
                                requester = allRequester,
                                narrow = narrow
                            )
                            NavAction(
                                title = stringResource(R.string.favorites_tab),
                                onClick = { onlyFavorites = true },
                                selected = onlyFavorites,
                                requester = favoritesRequester,
                                narrow = narrow
                            )
                            NavAction(
                                title = stringResource(R.string.settings),
                                onClick = { settingsVisible = true },
                                requester = settingsRequester,
                                narrow = narrow
                            )
                        }
                    }

                    Spacer(Modifier.height(if (short) 12.dp else 18.dp))

                    when {
                        loading -> MessageState(stringResource(R.string.loading))
                        ordered.isEmpty() -> MessageState(
                            stringResource(
                                if (catalogError) R.string.catalog_error
                                else if (onlyFavorites) R.string.no_favorites
                                else R.string.no_apps
                            )
                        )
                        else -> LazyVerticalGrid(
                            columns = GridCells.Fixed(if (narrow) 3 else 4),
                            state = gridState,
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(bottom = 10.dp),
                            horizontalArrangement = Arrangement.spacedBy(11.dp),
                            verticalArrangement = Arrangement.spacedBy(11.dp)
                        ) {
                            items(ordered, key = { it.component.flattenToString() }) { app ->
                                val key = app.component.flattenToString()
                                val requester = appRequesters.getOrPut(key) { FocusRequester() }
                                AppTile(
                                    app = app,
                                    requester = requester,
                                    height = tileHeight,
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
    }
}

@Composable
private fun BrandRail(
    modifier: Modifier,
    clock: String,
    meta: String,
    app: TvApp?,
    favorite: Boolean,
    title: String,
    footer: String,
    compact: Boolean,
    short: Boolean
) {
    Column(
        modifier = modifier
            .background(Rail, RoundedCornerShape(18.dp))
            .padding(horizontal = if (compact) 17.dp else 22.dp,
                vertical = if (short) 12.dp else 20.dp),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Column {
            Text(
                text = stringResource(R.string.app_name),
                color = Primary,
                fontSize = if (short) 17.sp else if (compact) 20.sp else 25.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = (-0.8).sp,
                maxLines = 1
            )
            Spacer(Modifier.height(if (short) 8.dp else 17.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(Hairline)
            )
            Spacer(Modifier.height(if (short) 8.dp else 21.dp))
            Text(
                text = clock,
                color = Primary,
                fontSize = if (short) 28.sp else if (compact) 38.sp else 44.sp,
                fontWeight = FontWeight.Light
            )
            Spacer(Modifier.height(3.dp))
            Text(
                text = meta,
                color = Secondary,
                fontSize = if (compact) 11.sp else 12.sp,
                lineHeight = if (short) 15.sp else 17.sp,
                maxLines = if (short) 2 else 3
            )
        }

        Column {
            if (app != null) {
                Text(
                    text = stringResource(R.string.selected_now),
                    color = Secondary,
                    fontSize = 12.sp
                )
                Spacer(Modifier.height(if (short) 7.dp else 14.dp))
                OriginalAppIcon(
                    app,
                    Modifier.size(if (short) 44.dp else if (compact) 60.dp else 72.dp)
                )
                Spacer(Modifier.height(if (short) 7.dp else 14.dp))
                Text(
                    text = app.title,
                    color = Primary,
                    fontSize = if (short) 17.sp else if (compact) 22.sp else 25.sp,
                    fontWeight = FontWeight.SemiBold,
                    lineHeight = if (short) 20.sp else if (compact) 26.sp else 30.sp,
                    maxLines = if (short) 1 else 2,
                    overflow = TextOverflow.Ellipsis
                )
                if (favorite) {
                    Spacer(Modifier.height(if (short) 2.dp else 5.dp))
                    Text(
                        text = stringResource(R.string.favorite),
                        color = Accent,
                        fontSize = 12.sp
                    )
                }
            } else {
                Text(
                    text = title,
                    color = Primary,
                    fontSize = if (short) 18.sp else if (compact) 22.sp else 25.sp,
                    lineHeight = if (short) 22.sp else 29.sp,
                    maxLines = if (short) 2 else 3
                )
            }
        }

        Text(
            text = footer,
            color = Secondary,
            fontSize = if (short) 10.sp else if (compact) 11.sp else 12.sp,
            lineHeight = if (short) 14.sp else 18.sp,
            maxLines = if (short) 2 else 4
        )
    }
}

@Composable
private fun OriginalAppIcon(app: TvApp, modifier: Modifier) {
    // The image source is the installed application's actual Drawable.
    // No new badges, forced icon backgrounds, clipping or color transforms.
    AndroidView(
        factory = { context ->
            ImageView(context).apply {
                scaleType = ImageView.ScaleType.FIT_CENTER
                adjustViewBounds = true
            }
        },
        update = { view ->
            view.setImageDrawable(
                app.originalIcon.constantState?.newDrawable()?.mutate() ?: app.originalIcon
            )
        },
        modifier = modifier
    )
}

@Composable
private fun MessageState(message: String) {
    Box(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = message,
            color = Primary,
            fontSize = 20.sp,
            lineHeight = 29.sp,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun AppTile(
    app: TvApp,
    requester: FocusRequester,
    height: Dp,
    compact: Boolean,
    isFavorite: Boolean,
    onFocused: () -> Unit,
    onLaunch: () -> Unit,
    onToggleFavorite: () -> Unit
) {
    var focused by remember { mutableStateOf(false) }
    val shape = RoundedCornerShape(14.dp)
    val favoriteLabel = stringResource(R.string.favorite)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .height(height)
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
            .background(if (focused) SurfaceFocused else Surface, shape)
            .border(
                width = if (focused) 2.dp else 0.dp,
                color = if (focused) Accent else Color.Transparent,
                shape = shape
            )
            .semantics {
                contentDescription = app.title + if (isFavorite) " " + favoriteLabel else ""
            }
            .clickable(onClick = onLaunch)
            .padding(horizontal = 14.dp, vertical = 11.dp),
        horizontalAlignment = Alignment.Start
    ) {
        Box(
            modifier = Modifier.fillMaxWidth().weight(1f),
            contentAlignment = Alignment.CenterStart
        ) {
            OriginalAppIcon(
                app,
                Modifier.size(if (compact) 54.dp else 62.dp)
            )
        }
        Text(
            text = app.title,
            color = Primary,
            fontSize = if (compact) 15.sp else 16.sp,
            fontWeight = if (focused) FontWeight.SemiBold else FontWeight.Medium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            text = if (isFavorite) favoriteLabel else " ",
            color = Accent,
            fontSize = 11.sp,
            maxLines = 1
        )
    }
}

@Composable
private fun SettingsScreen(
    modifier: Modifier,
    options: LauncherOptions,
    short: Boolean,
    onOptionsChange: (LauncherOptions) -> Unit,
    onClose: () -> Unit,
    onOpenAndroidSettings: () -> Unit
) {
    val doneRequester = remember { FocusRequester() }
    LaunchedEffect(Unit) {
        withFrameNanos { }
        doneRequester.requestFocus()
    }

    Column(modifier = modifier.verticalScroll(rememberScrollState())) {
        Row(
            modifier = Modifier.fillMaxWidth().height(56.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = stringResource(R.string.settings),
                color = Primary,
                fontSize = if (short) 21.sp else 27.sp,
                fontWeight = FontWeight.SemiBold
            )
            NavAction(
                title = stringResource(R.string.done),
                onClick = onClose,
                requester = doneRequester
            )
        }
        Spacer(Modifier.height(if (short) 10.dp else 18.dp))
        PreferenceTile(
            short = short,
            title = stringResource(R.string.compact_grid),
            description = stringResource(R.string.compact_grid_description),
            value = stringResource(if (options.compactGrid) R.string.enabled else R.string.disabled),
            onClick = { onOptionsChange(options.copy(compactGrid = !options.compactGrid)) }
        )
        Spacer(Modifier.height(if (short) 8.dp else 12.dp))
        PreferenceTile(
            short = short,
            title = stringResource(R.string.favorites_first),
            description = stringResource(R.string.favorites_first_description),
            value = stringResource(if (options.favoritesFirst) R.string.enabled else R.string.disabled),
            onClick = { onOptionsChange(options.copy(favoritesFirst = !options.favoritesFirst)) }
        )
        Spacer(Modifier.height(if (short) 8.dp else 12.dp))
        PreferenceTile(
            short = short,
            title = stringResource(R.string.android_settings),
            description = stringResource(R.string.android_settings_description),
            value = "",
            onClick = onOpenAndroidSettings
        )
        Spacer(Modifier.height(24.dp))
        Text(
            text = stringResource(R.string.settings_saved),
            color = Secondary,
            fontSize = 13.sp,
            lineHeight = 19.sp
        )
    }
}

@Composable
private fun PreferenceTile(
    short: Boolean,
    title: String,
    description: String,
    value: String,
    onClick: () -> Unit
) {
    var focused by remember { mutableStateOf(false) }
    val shape = RoundedCornerShape(14.dp)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(if (short) 82.dp else 98.dp)
            .onFocusChanged { focused = it.isFocused }
            .background(if (focused) SurfaceFocused else Surface, shape)
            .border(
                width = if (focused) 2.dp else 0.dp,
                color = if (focused) Accent else Color.Transparent,
                shape = shape
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 22.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                color = Primary,
                fontSize = if (short) 16.sp else 18.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.height(5.dp))
            Text(
                text = description,
                color = Secondary,
                fontSize = if (short) 12.sp else 13.sp,
                lineHeight = if (short) 16.sp else 18.sp,
                maxLines = if (short) 1 else 2,
                overflow = TextOverflow.Ellipsis
            )
        }
        if (value.isNotEmpty()) {
            Spacer(Modifier.width(14.dp))
            Text(
                text = value,
                color = if (focused) Primary else Secondary,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
private fun NavAction(
    title: String,
    onClick: () -> Unit,
    selected: Boolean = false,
    requester: FocusRequester? = null,
    narrow: Boolean = false
) {
    var focused by remember { mutableStateOf(false) }
    val shape = RoundedCornerShape(12.dp)
    Box(
        modifier = Modifier
            .then(if (requester == null) Modifier else Modifier.focusRequester(requester))
            .onFocusChanged { focused = it.isFocused }
            .background(
                if (focused) SurfaceFocused else if (selected) Surface else Color.Transparent,
                shape
            )
            .border(
                width = if (focused) 2.dp else 0.dp,
                color = if (focused) Accent else Color.Transparent,
                shape = shape
            )
            .clickable(onClick = onClick)
            .padding(
                horizontal = if (narrow) 10.dp else 14.dp,
                vertical = 11.dp
            ),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = title,
            color = if (selected || focused) Primary else Secondary,
            fontSize = if (narrow) 13.sp else 14.sp,
            fontWeight = if (selected || focused) FontWeight.SemiBold else FontWeight.Medium,
            maxLines = 1
        )
    }
}

private val TimeFormat = DateTimeFormatter.ofPattern("HH:mm")
private fun formattedTime(): String = LocalTime.now().format(TimeFormat)
