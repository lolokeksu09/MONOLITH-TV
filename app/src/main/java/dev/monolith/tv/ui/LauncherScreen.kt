package dev.monolith.tv.ui

import android.view.KeyEvent
import android.widget.ImageView
import androidx.activity.compose.BackHandler
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
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
import androidx.compose.ui.graphics.graphicsLayer
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
import androidx.compose.ui.zIndex
import androidx.tv.material3.Text
import dev.monolith.tv.R
import dev.monolith.tv.catalog.AppOrder
import dev.monolith.tv.catalog.CatalogFilter
import dev.monolith.tv.catalog.LauncherOptions
import dev.monolith.tv.catalog.TvApp
import kotlinx.coroutines.delay
import java.time.LocalTime
import java.time.format.DateTimeFormatter

// MONOLITH TV 0.1.3 — true AMOLED black, no wallpapers, fake content or icon packs.
// Accent belongs to navigation and TV remote focus, never to installed app artwork.
private val Black = Color(0xFF000000)
private val Surface = Color(0xFF0B0F13)
private val SurfaceHigh = Color(0xFF11212A)
private val HeroSurface = Color(0xFF10161B)
private val Foreground = Color(0xFFF5FAFC)
private val Muted = Color(0xFFB2C0C7)
private val Quiet = Color(0xFF84949C)
private val Focus = Color(0xFFB7F3FF)
private val Hairline = Color(0xFF25323A)
private val FocusTween = tween<Float>(durationMillis = 140, easing = FastOutSlowInEasing)

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
    var favoritesOnly by rememberSaveable { mutableStateOf(false) }
    var settingsVisible by rememberSaveable { mutableStateOf(false) }
    var lastFocusedKey by rememberSaveable { mutableStateOf<String?>(null) }
    var pendingFocusKey by remember { mutableStateOf<String?>(null) }

    val ordered = remember(apps, favoritePackages, favoritesOnly, options.favoritesFirst) {
        AppOrder.visible(
            apps, favoritePackages, { it.packageName }, { it.title },
            if (favoritesOnly) CatalogFilter.FAVORITES else CatalogFilter.ALL,
            options.favoritesFirst
        )
    }
    val favoriteCount = remember(apps, favoritePackages) {
        apps.count { it.packageName in favoritePackages }
    }
    val selectedApp = remember(ordered, lastFocusedKey) {
        ordered.firstOrNull { it.component.flattenToString() == lastFocusedKey }
            ?: ordered.firstOrNull()
    }

    val gridState = rememberLazyGridState()
    val appRequesters = remember { mutableMapOf<String, FocusRequester>() }
    val allRequester = remember { FocusRequester() }
    val favoriteRequester = remember { FocusRequester() }
    val settingsRequester = remember { FocusRequester() }

    val clock by produceState(initialValue = formattedTime()) {
        while (true) {
            value = formattedTime()
            delay(15_000)
        }
    }

    BackHandler(enabled = settingsVisible) { settingsVisible = false }
    BackHandler(enabled = !settingsVisible && favoritesOnly) { favoritesOnly = false }

    // Restore by stable component key, not the current visual index. MENU may reorder
    // a favorite, and removing the final favorite must keep the remote usable.
    LaunchedEffect(loading, settingsVisible, favoritesOnly, ordered) {
        if (loading || settingsVisible) return@LaunchedEffect
        if (ordered.isEmpty()) {
            withFrameNanos { }
            if (favoritesOnly) favoriteRequester.requestFocus()
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

    BoxWithConstraints(Modifier.fillMaxSize().background(Black)) {
        val spec = TvLayoutSpec.forViewport(maxWidth.value, maxHeight.value, options.compactGrid)

        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = spec.edgeDp.dp, vertical = if (spec.short) 14.dp else 22.dp),
            horizontalArrangement = Arrangement.spacedBy(spec.gapDp.dp)
        ) {
            NavigationRail(
                modifier = Modifier.width(spec.railDp.dp).fillMaxHeight(),
                clock = clock,
                appCount = apps.size,
                favoriteCount = favoriteCount,
                favoritesOnly = favoritesOnly,
                settingsVisible = settingsVisible,
                compact = spec.short,
                allRequester = allRequester,
                favoriteRequester = favoriteRequester,
                settingsRequester = settingsRequester,
                onAll = { favoritesOnly = false; settingsVisible = false },
                onFavorites = { favoritesOnly = true; settingsVisible = false },
                onSettings = { settingsVisible = true }
            )

            Column(Modifier.weight(1f).fillMaxHeight()) {
                if (settingsVisible) {
                    SettingsPanel(
                        options = options,
                        short = spec.short,
                        onOptionsChange = onOptionsChange,
                        onClose = { settingsVisible = false },
                        onOpenAndroidSettings = onOpenAndroidSettings
                    )
                } else {
                    FeaturedPanel(
                        modifier = Modifier.fillMaxWidth().height(spec.heroDp.dp),
                        app = selectedApp,
                        isFavorite = selectedApp?.packageName in favoritePackages,
                        favoritesOnly = favoritesOnly,
                        compact = spec.short
                    )
                    Spacer(Modifier.height(if (spec.short) 9.dp else 12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth().height(28.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = stringResource(
                                if (favoritesOnly) R.string.favorites_tab else R.string.apps_heading
                            ),
                            color = Foreground,
                            fontSize = if (spec.short) 18.sp else 20.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = stringResource(R.string.item_count, ordered.size),
                            color = Muted,
                            fontSize = 12.sp
                        )
                    }
                    Spacer(Modifier.height(8.dp))
                    when {
                        loading -> EmptyState(stringResource(R.string.loading))
                        ordered.isEmpty() -> EmptyState(
                            stringResource(
                                if (catalogError) R.string.catalog_error
                                else if (favoritesOnly) R.string.no_favorites
                                else R.string.no_apps
                            )
                        )
                        else -> LazyVerticalGrid(
                            columns = GridCells.Fixed(spec.columns),
                            state = gridState,
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(
                                start = 3.dp, end = 3.dp, top = 4.dp, bottom = 10.dp
                            ),
                            horizontalArrangement = Arrangement.spacedBy(spec.tileGapDp.dp),
                            verticalArrangement = Arrangement.spacedBy(spec.tileGapDp.dp)
                        ) {
                            items(ordered, key = { it.component.flattenToString() }) { app ->
                                val key = app.component.flattenToString()
                                val requester = remember(key) { FocusRequester() }
                                DisposableEffect(key, requester) {
                                    appRequesters[key] = requester
                                    onDispose {
                                        if (appRequesters[key] === requester) appRequesters.remove(key)
                                    }
                                }
                                AppCard(
                                    app = app,
                                    requester = requester,
                                    height = spec.tileDp.dp,
                                    short = spec.short,
                                    favorite = app.packageName in favoritePackages,
                                    onFocused = { lastFocusedKey = key },
                                    onOpen = { onLaunch(app) },
                                    onFavorite = {
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
private fun NavigationRail(
    modifier: Modifier,
    clock: String,
    appCount: Int,
    favoriteCount: Int,
    favoritesOnly: Boolean,
    settingsVisible: Boolean,
    compact: Boolean,
    allRequester: FocusRequester,
    favoriteRequester: FocusRequester,
    settingsRequester: FocusRequester,
    onAll: () -> Unit,
    onFavorites: () -> Unit,
    onSettings: () -> Unit
) {
    Column(modifier) {
        Text(
            text = stringResource(R.string.app_name),
            color = Foreground,
            fontSize = if (compact) 17.sp else 21.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = (-0.7).sp,
            maxLines = 1
        )
        Spacer(Modifier.height(if (compact) 16.dp else 32.dp))
        RailItem(
            label = stringResource(R.string.all_apps),
            count = appCount,
            selected = !favoritesOnly && !settingsVisible,
            compact = compact,
            requester = allRequester,
            onClick = onAll
        )
        Spacer(Modifier.height(5.dp))
        RailItem(
            label = stringResource(R.string.favorites_tab),
            count = favoriteCount,
            selected = favoritesOnly && !settingsVisible,
            compact = compact,
            requester = favoriteRequester,
            onClick = onFavorites
        )
        Spacer(Modifier.height(5.dp))
        RailItem(
            label = stringResource(R.string.settings),
            count = null,
            selected = settingsVisible,
            compact = compact,
            requester = settingsRequester,
            onClick = onSettings
        )
        Spacer(Modifier.weight(1f))
        Text(
            text = clock,
            color = Foreground,
            fontSize = if (compact) 30.sp else 40.sp,
            fontWeight = FontWeight.Light,
            maxLines = 1
        )
        Spacer(Modifier.height(5.dp))
        Text(
            text = stringResource(R.string.remote_hint),
            color = Quiet,
            fontSize = if (compact) 10.sp else 11.sp,
            lineHeight = if (compact) 13.sp else 16.sp,
            maxLines = if (compact) 2 else 3,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun RailItem(
    label: String,
    count: Int?,
    selected: Boolean,
    compact: Boolean,
    requester: FocusRequester,
    onClick: () -> Unit
) {
    var focused by remember { mutableStateOf(false) }
    val fill by animateColorAsState(
        targetValue = if (focused) SurfaceHigh else if (selected) Surface else Black,
        animationSpec = tween(140), label = "rail-surface"
    )
    val radius = RoundedCornerShape(12.dp)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(if (compact) 43.dp else 50.dp)
            .focusRequester(requester)
            .onFocusChanged { focused = it.isFocused }
            .background(fill, radius)
            .border(
                if (focused) 1.5.dp else 0.dp,
                if (focused) Focus else Color.Transparent,
                radius
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            Modifier
                .width(3.dp)
                .height(20.dp)
                .background(if (selected || focused) Focus else Color.Transparent,
                    RoundedCornerShape(2.dp))
        )
        Spacer(Modifier.width(9.dp))
        Text(
            text = label,
            color = if (focused || selected) Foreground else Muted,
            fontSize = if (compact) 13.sp else 15.sp,
            fontWeight = if (focused || selected) FontWeight.SemiBold else FontWeight.Medium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )
        if (count != null) {
            Spacer(Modifier.width(4.dp))
            Text(
                text = count.toString(),
                color = Quiet,
                fontSize = if (compact) 11.sp else 12.sp
            )
        }
    }
}

@Composable
private fun FeaturedPanel(
    modifier: Modifier,
    app: TvApp?,
    isFavorite: Boolean,
    favoritesOnly: Boolean,
    compact: Boolean
) {
    Row(
        modifier = modifier
            .background(HeroSurface, RoundedCornerShape(21.dp))
            .padding(horizontal = if (compact) 18.dp else 26.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.Center) {
            Text(
                text = stringResource(
                    if (app != null) R.string.selected_now
                    else if (favoritesOnly) R.string.favorites_tab
                    else R.string.apps_heading
                ),
                color = Focus,
                fontSize = if (compact) 10.sp else 12.sp,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(Modifier.height(if (compact) 5.dp else 8.dp))
            Text(
                text = app?.title ?: stringResource(
                    if (favoritesOnly) R.string.no_favorites_title else R.string.app_name
                ),
                color = Foreground,
                fontSize = if (compact) 24.sp else 33.sp,
                lineHeight = if (compact) 28.sp else 38.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            if (!compact) {
                Spacer(Modifier.height(7.dp))
                Text(
                    text = stringResource(
                        if (app != null && isFavorite) R.string.favorite
                        else R.string.open_with_ok
                    ),
                    color = Muted,
                    fontSize = 13.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
        if (app != null) {
            Spacer(Modifier.width(16.dp))
            OriginalAppIcon(
                app = app,
                modifier = Modifier.size(if (compact) 57.dp else 80.dp)
            )
        }
    }
}

@Composable
private fun OriginalAppIcon(app: TvApp, modifier: Modifier) {
    // Render the Android-installed original Drawable, without masking or tint.
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
private fun AppCard(
    app: TvApp,
    requester: FocusRequester,
    height: Dp,
    short: Boolean,
    favorite: Boolean,
    onFocused: () -> Unit,
    onOpen: () -> Unit,
    onFavorite: () -> Unit
) {
    var focused by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (focused) 1.035f else 1f,
        animationSpec = FocusTween, label = "app-focus-scale"
    )
    val surfaceColor by animateColorAsState(
        targetValue = if (focused) SurfaceHigh else Surface,
        animationSpec = tween(140), label = "app-focus-surface"
    )
    val shape = RoundedCornerShape(16.dp)
    val favoriteLabel = stringResource(R.string.favorite)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .height(height)
            .zIndex(if (focused) 1f else 0f)
            .graphicsLayer(scaleX = scale, scaleY = scale)
            .focusRequester(requester)
            .onFocusChanged {
                focused = it.isFocused
                if (it.isFocused) onFocused()
            }
            .onPreviewKeyEvent { event ->
                if (event.type == KeyEventType.KeyUp &&
                    event.nativeKeyEvent.keyCode == KeyEvent.KEYCODE_MENU
                ) {
                    onFavorite()
                    true
                } else false
            }
            .background(surfaceColor, shape)
            .border(
                if (focused) 2.dp else 0.dp,
                if (focused) Focus else Color.Transparent,
                shape
            )
            .semantics {
                contentDescription = app.title + if (favorite) " " + favoriteLabel else ""
            }
            .clickable(onClick = onOpen)
            .padding(horizontal = 12.dp, vertical = if (short) 9.dp else 10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().weight(1f),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top
        ) {
            OriginalAppIcon(
                app,
                Modifier.size(if (short) 40.dp else 47.dp)
            )
            if (favorite) {
                Text(
                    text = "★",
                    color = Focus,
                    fontSize = 17.sp,
                    modifier = Modifier.padding(top = 1.dp)
                )
            }
        }
        Text(
            text = app.title,
            color = Foreground,
            fontSize = if (short) 13.sp else 14.sp,
            fontWeight = if (focused) FontWeight.SemiBold else FontWeight.Medium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun SettingsPanel(
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

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        Row(
            modifier = Modifier.fillMaxWidth().height(if (short) 70.dp else 90.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Text(
                    stringResource(R.string.launcher_settings),
                    color = Foreground,
                    fontSize = if (short) 24.sp else 31.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(3.dp))
                Text(
                    stringResource(R.string.settings_subtitle),
                    color = Muted,
                    fontSize = 12.sp
                )
            }
            Spacer(Modifier.width(12.dp))
            SettingsDone(
                requester = doneRequester,
                label = stringResource(R.string.done),
                onClick = onClose
            )
        }
        Spacer(Modifier.height(if (short) 8.dp else 17.dp))
        SettingRow(
            title = stringResource(R.string.compact_grid),
            description = stringResource(R.string.compact_grid_description),
            value = stringResource(if (options.compactGrid) R.string.enabled else R.string.disabled),
            short = short,
            onClick = { onOptionsChange(options.copy(compactGrid = !options.compactGrid)) }
        )
        Spacer(Modifier.height(10.dp))
        SettingRow(
            title = stringResource(R.string.favorites_first),
            description = stringResource(R.string.favorites_first_description),
            value = stringResource(if (options.favoritesFirst) R.string.enabled else R.string.disabled),
            short = short,
            onClick = { onOptionsChange(options.copy(favoritesFirst = !options.favoritesFirst)) }
        )
        Spacer(Modifier.height(10.dp))
        SettingRow(
            title = stringResource(R.string.android_settings),
            description = stringResource(R.string.android_settings_description),
            value = "",
            short = short,
            onClick = onOpenAndroidSettings
        )
        Spacer(Modifier.height(14.dp))
        Text(
            stringResource(R.string.settings_saved),
            color = Quiet,
            fontSize = 12.sp,
            lineHeight = 17.sp
        )
    }
}

@Composable
private fun SettingRow(
    title: String,
    description: String,
    value: String,
    short: Boolean,
    onClick: () -> Unit
) {
    var focused by remember { mutableStateOf(false) }
    val fill by animateColorAsState(
        targetValue = if (focused) SurfaceHigh else Surface,
        animationSpec = tween(140), label = "setting-surface"
    )
    val shape = RoundedCornerShape(16.dp)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(if (short) 77.dp else 95.dp)
            .onFocusChanged { focused = it.isFocused }
            .background(fill, shape)
            .border(
                if (focused) 2.dp else 0.dp,
                if (focused) Focus else Color.Transparent,
                shape
            )
            .clickable(onClick = onClick)
            .padding(horizontal = if (short) 14.dp else 20.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Text(
                title,
                color = Foreground,
                fontSize = if (short) 16.sp else 18.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1, overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.height(4.dp))
            Text(
                description,
                color = Muted,
                fontSize = if (short) 11.sp else 13.sp,
                maxLines = if (short) 1 else 2,
                lineHeight = if (short) 14.sp else 17.sp,
                overflow = TextOverflow.Ellipsis
            )
        }
        if (value.isNotEmpty()) {
            Spacer(Modifier.width(15.dp))
            Box(
                Modifier
                    .background(
                        if (focused) Color(0xFF254957) else Color(0xFF1B282E),
                        RoundedCornerShape(20.dp)
                    )
                    .padding(horizontal = 13.dp, vertical = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    value,
                    color = if (focused) Focus else Foreground,
                    fontSize = if (short) 12.sp else 13.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

@Composable
private fun SettingsDone(
    requester: FocusRequester,
    label: String,
    onClick: () -> Unit
) {
    var focused by remember { mutableStateOf(false) }
    val shape = RoundedCornerShape(12.dp)
    Box(
        modifier = Modifier
            .focusRequester(requester)
            .onFocusChanged { focused = it.isFocused }
            .background(if (focused) SurfaceHigh else Surface, shape)
            .border(
                if (focused) 2.dp else 1.dp,
                if (focused) Focus else Hairline, shape
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 18.dp, vertical = 11.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(label, color = Foreground, fontSize = 14.sp)
    }
}

@Composable
private fun EmptyState(message: String) {
    Box(
        Modifier.fillMaxSize().padding(28.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            message,
            color = Muted,
            fontSize = 18.sp,
            lineHeight = 25.sp,
            textAlign = TextAlign.Center
        )
    }
}

private val TimeFormat = DateTimeFormatter.ofPattern("HH:mm")
private fun formattedTime(): String = LocalTime.now().format(TimeFormat)
