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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.graphics.Brush
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

/**
 * MONOLITH TV 0.1.4 / CINEMA.
 * Full-width feature stage + ONE horizontal focus-driven app dock.
 * Zero generic multi-row cards and zero persistent sidebar.
 *
 * Android's actual app Drawable is always used unchanged. The ambient
 * light behind the selected app is a simple native Compose Brush.
 */
private val Night = Color(0xFF000000)
private val White = Color(0xFFF9FAFF)
private val Silver = Color(0xFFC9D0E4)
private val Soft = Color(0xFF8891AD)
private val Ink = Color(0xFF15152A)
private val Select = Color(0xFFD5DCFF)
private val Iris = Color(0xFF9EA9FF)
private val StageBrush = Brush.horizontalGradient(
    listOf(Color(0xFF111124), Color(0xFF191A3B), Color(0xFF101B31))
)
private val HaloBrush = Brush.radialGradient(
    listOf(Color(0x885E83FF), Color(0x5558BCFF), Color.Transparent)
)
private val FocusMotion = tween<Float>(durationMillis = 165, easing = FastOutSlowInEasing)

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
    var focusedKey by rememberSaveable { mutableStateOf<String?>(null) }
    var restoreKey by remember { mutableStateOf<String?>(null) }

    val visible = remember(apps, favoritePackages, favoritesOnly, options.favoritesFirst) {
        AppOrder.visible(
            apps, favoritePackages, { it.packageName }, { it.title },
            if (favoritesOnly) CatalogFilter.FAVORITES else CatalogFilter.ALL,
            options.favoritesFirst
        )
    }
    val favoriteCount = remember(apps, favoritePackages) {
        apps.count { it.packageName in favoritePackages }
    }
    val featured = remember(visible, focusedKey) {
        visible.firstOrNull { it.component.flattenToString() == focusedKey }
            ?: visible.firstOrNull()
    }

    val shelfState = rememberLazyListState()
    val tileRequesters = remember { mutableMapOf<String, FocusRequester>() }
    val allTab = remember { FocusRequester() }
    val favoritesTab = remember { FocusRequester() }
    val settingsTab = remember { FocusRequester() }

    val clock by produceState(initialValue = formattedTime()) {
        while (true) {
            value = formattedTime()
            delay(15_000)
        }
    }

    fun returnToDock() {
        val key = featured?.component?.flattenToString() ?: return
        tileRequesters[key]?.requestFocus()
    }

    BackHandler(enabled = settingsVisible) { settingsVisible = false }
    BackHandler(enabled = !settingsVisible && favoritesOnly) { favoritesOnly = false }

    // Follow stable component identity when favorites re-sort the dock. When
    // the last favorite disappears, put focus on its still-visible tab.
    LaunchedEffect(visible, settingsVisible, loading, favoritesOnly) {
        if (settingsVisible || loading) return@LaunchedEffect
        if (visible.isEmpty()) {
            withFrameNanos { }
            if (favoritesOnly) favoritesTab.requestFocus() else allTab.requestFocus()
            restoreKey = null
            return@LaunchedEffect
        }
        val key = (restoreKey ?: focusedKey)
            ?.takeIf { candidate ->
                visible.any { it.component.flattenToString() == candidate }
            } ?: visible.first().component.flattenToString()
        val index = visible.indexOfFirst { it.component.flattenToString() == key }
        shelfState.scrollToItem(index)
        withFrameNanos { }
        withFrameNanos { }
        tileRequesters[key]?.requestFocus()
        restoreKey = null
    }

    BoxWithConstraints(Modifier.fillMaxSize().background(Night)) {
        val spec = TvLayoutSpec.forViewport(maxWidth.value, maxHeight.value, options.compactGrid)

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = spec.edgeDp.dp, vertical = spec.verticalDp.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(spec.headerDp.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Wordmark(short = spec.short)
                Spacer(Modifier.weight(1f))
                Row(horizontalArrangement = Arrangement.spacedBy(if (spec.narrow) 3.dp else 8.dp)) {
                    HeaderTab(
                        text = stringResource(R.string.all_apps),
                        active = !favoritesOnly && !settingsVisible,
                        requester = allTab,
                        short = spec.short,
                        onDown = ::returnToDock,
                        onClick = {
                            settingsVisible = false
                            favoritesOnly = false
                        }
                    )
                    HeaderTab(
                        text = stringResource(R.string.favorites_tab),
                        active = favoritesOnly && !settingsVisible,
                        requester = favoritesTab,
                        short = spec.short,
                        onDown = ::returnToDock,
                        onClick = {
                            settingsVisible = false
                            favoritesOnly = true
                        }
                    )
                    HeaderTab(
                        text = stringResource(R.string.settings),
                        active = settingsVisible,
                        requester = settingsTab,
                        short = spec.short,
                        onDown = ::returnToDock,
                        onClick = { settingsVisible = true }
                    )
                }
                Spacer(Modifier.width(if (spec.narrow) 11.dp else 22.dp))
                Text(
                    text = clock,
                    color = White,
                    fontSize = if (spec.short) 18.sp else 22.sp,
                    fontWeight = FontWeight.Light,
                    maxLines = 1
                )
            }

            Spacer(Modifier.height(spec.headerGapDp.dp))

            if (settingsVisible) {
                SettingsStage(
                    options = options,
                    short = spec.short,
                    modifier = Modifier.fillMaxSize(),
                    onChange = onOptionsChange,
                    onSystemSettings = onOpenAndroidSettings,
                    onClose = { settingsVisible = false }
                )
            } else {
                FeatureStage(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(spec.stageDp.dp),
                    app = featured,
                    favoritesOnly = favoritesOnly,
                    favorite = featured?.packageName in favoritePackages,
                    short = spec.short
                )

                Spacer(Modifier.height(spec.stageGapDp.dp))

                Row(
                    modifier = Modifier.fillMaxWidth().height(spec.sectionDp.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(
                            if (favoritesOnly) R.string.favorites_tab else R.string.apps_heading
                        ),
                        color = White,
                        fontSize = if (spec.short) 17.sp else 20.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(Modifier.width(14.dp))
                    Text(
                        text = stringResource(R.string.item_count, visible.size),
                        color = Soft,
                        fontSize = if (spec.short) 11.sp else 13.sp
                    )
                    Spacer(Modifier.weight(1f))
                    if (!spec.short && visible.isNotEmpty()) {
                        Text(
                            text = stringResource(R.string.dock_hint),
                            color = Soft,
                            fontSize = 12.sp
                        )
                    }
                }
                Spacer(Modifier.height(spec.shelfGapDp.dp))

                when {
                    loading -> DockMessage(stringResource(R.string.loading))
                    visible.isEmpty() -> DockMessage(
                        stringResource(
                            if (catalogError) R.string.catalog_error
                            else if (favoritesOnly) R.string.no_favorites
                            else R.string.no_apps
                        )
                    )
                    else -> LazyRow(
                        state = shelfState,
                        modifier = Modifier.fillMaxWidth().height(spec.shelfDp.dp),
                        contentPadding = PaddingValues(
                            start = 5.dp, end = 18.dp, top = 5.dp, bottom = 6.dp
                        ),
                        horizontalArrangement = Arrangement.spacedBy(
                            if (spec.short) 9.dp else 14.dp
                        )
                    ) {
                        items(visible, key = { it.component.flattenToString() }) { app ->
                            val key = app.component.flattenToString()
                            val requester = remember(key) { FocusRequester() }
                            DisposableEffect(key, requester) {
                                tileRequesters[key] = requester
                                onDispose {
                                    if (tileRequesters[key] === requester) {
                                        tileRequesters.remove(key)
                                    }
                                }
                            }
                            DockTile(
                                app = app,
                                requester = requester,
                                width = spec.cardDp.dp,
                                height = (spec.shelfDp - 12).dp,
                                iconSize = spec.iconDp.dp,
                                short = spec.short,
                                favorite = app.packageName in favoritePackages,
                                onUp = {
                                    if (favoritesOnly) favoritesTab.requestFocus()
                                    else allTab.requestFocus()
                                },
                                onFocused = { focusedKey = key },
                                onOpen = { onLaunch(app) },
                                onFavorite = {
                                    restoreKey = key
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

@Composable
private fun Wordmark(short: Boolean) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(if (short) 27.dp else 32.dp)
                .background(
                    Brush.linearGradient(
                        listOf(Color(0xFF808DFF), Color(0xFF4C59B7))
                    ),
                    RoundedCornerShape(9.dp)
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "M",
                color = White,
                fontWeight = FontWeight.Black,
                fontSize = if (short) 19.sp else 22.sp
            )
        }
        Spacer(Modifier.width(if (short) 7.dp else 10.dp))
        Text(
            text = "MONOLITH",
            fontWeight = FontWeight.Bold,
            color = White,
            fontSize = if (short) 17.sp else 21.sp,
            letterSpacing = (-0.8).sp,
            maxLines = 1
        )
    }
}

@Composable
private fun HeaderTab(
    text: String,
    active: Boolean,
    requester: FocusRequester,
    short: Boolean,
    onDown: () -> Unit,
    onClick: () -> Unit
) {
    var focused by remember { mutableStateOf(false) }
    val surface by animateColorAsState(
        targetValue = when {
            focused -> Color(0xFF29316B)
            active -> Color(0xFF191D3B)
            else -> Color.Transparent
        },
        animationSpec = tween(150), label = "top-tab"
    )
    val shape = RoundedCornerShape(12.dp)
    Box(
        modifier = Modifier
            .focusRequester(requester)
            .onFocusChanged { focused = it.isFocused }
            .onPreviewKeyEvent { event ->
                if (event.type == KeyEventType.KeyDown &&
                    event.nativeKeyEvent.keyCode == KeyEvent.KEYCODE_DPAD_DOWN
                ) {
                    onDown()
                    true
                } else false
            }
            .background(surface, shape)
            .border(
                if (focused) 1.5.dp else 0.dp,
                if (focused) Select else Color.Transparent, shape
            )
            .clickable(onClick = onClick)
            .padding(
                horizontal = if (short) 9.dp else 14.dp,
                vertical = if (short) 8.dp else 11.dp
            ),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = if (focused || active) White else Silver,
            fontSize = if (short) 12.sp else 14.sp,
            fontWeight = if (active) FontWeight.SemiBold else FontWeight.Medium,
            maxLines = 1
        )
    }
}

@Composable
private fun FeatureStage(
    modifier: Modifier,
    app: TvApp?,
    favoritesOnly: Boolean,
    favorite: Boolean,
    short: Boolean
) {
    val stageShape = RoundedCornerShape(if (short) 21.dp else 30.dp)
    Box(modifier = modifier.background(StageBrush, stageShape)) {
        // A single soft ambient halo belongs to the app's own artwork.
        // The artwork itself is displayed unchanged in the foreground.
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(
                    start = if (short) 22.dp else 42.dp,
                    end = if (short) 10.dp else 29.dp
                ),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.studio_ready),
                    color = Iris,
                    fontSize = if (short) 11.sp else 14.sp,
                    fontWeight = FontWeight.Medium
                )
                Spacer(Modifier.height(if (short) 6.dp else 12.dp))
                Text(
                    text = app?.title ?: stringResource(
                        if (favoritesOnly) R.string.no_favorites_title
                        else R.string.app_name
                    ),
                    color = White,
                    fontSize = if (short) 28.sp else 47.sp,
                    lineHeight = if (short) 32.sp else 54.sp,
                    letterSpacing = if (short) (-0.8).sp else (-1.4).sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(if (short) 9.dp else 20.dp))
                if (app == null) {
                    Text(
                        text = stringResource(
                            if (favoritesOnly) R.string.no_favorites
                            else R.string.loading
                        ),
                        color = Silver,
                        fontSize = if (short) 11.sp else 14.sp,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                } else {
                    Row(horizontalArrangement = Arrangement.spacedBy(if (short) 10.dp else 20.dp)) {
                        KeyHint("OK", stringResource(R.string.launch_short), short)
                        KeyHint(
                            "MENU",
                            stringResource(
                                if (favorite) R.string.remove_favorite_short
                                else R.string.favorite_short
                            ),
                            short
                        )
                    }
                }
            }
            Spacer(Modifier.width(if (short) 9.dp else 24.dp))
            Box(
                modifier = Modifier.size(if (short) 113.dp else 220.dp),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(HaloBrush, CircleShape)
                )
                if (app != null) {
                    OriginalAppIcon(
                        app = app,
                        modifier = Modifier.size(if (short) 85.dp else 148.dp)
                    )
                } else {
                    Text("M", color = Color(0xFF6774B5), fontSize = 70.sp)
                }
            }
        }
    }
}

@Composable
private fun KeyHint(key: String, value: String, short: Boolean) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .background(Color(0xFF343A60), RoundedCornerShape(7.dp))
                .padding(
                    horizontal = if (short) 7.dp else 9.dp,
                    vertical = if (short) 4.dp else 6.dp
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = key,
                color = White,
                fontSize = if (short) 10.sp else 12.sp,
                fontWeight = FontWeight.Bold
            )
        }
        Spacer(Modifier.width(if (short) 5.dp else 8.dp))
        Text(
            text = value,
            color = Silver,
            fontSize = if (short) 10.sp else 13.sp,
            maxLines = 1
        )
    }
}

@Composable
private fun OriginalAppIcon(app: TvApp, modifier: Modifier) {
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
private fun DockTile(
    app: TvApp,
    requester: FocusRequester,
    width: Dp,
    height: Dp,
    iconSize: Dp,
    short: Boolean,
    favorite: Boolean,
    onUp: () -> Unit,
    onFocused: () -> Unit,
    onOpen: () -> Unit,
    onFavorite: () -> Unit
) {
    var focused by remember { mutableStateOf(false) }
    val zoom by animateFloatAsState(
        targetValue = if (focused) 1.065f else 1f,
        animationSpec = FocusMotion, label = "dock-zoom"
    )
    val surface by animateColorAsState(
        targetValue = if (focused) Color(0xFF212B62) else Color.Transparent,
        animationSpec = tween(165), label = "dock-selection"
    )
    val shape = RoundedCornerShape(if (short) 13.dp else 20.dp)
    val favoriteLabel = stringResource(R.string.favorite)

    Column(
        modifier = Modifier
            .width(width)
            .height(height)
            .zIndex(if (focused) 1f else 0f)
            .graphicsLayer(scaleX = zoom, scaleY = zoom)
            .focusRequester(requester)
            .onFocusChanged {
                focused = it.isFocused
                if (it.isFocused) onFocused()
            }
            .onPreviewKeyEvent { event ->
                when {
                    event.type == KeyEventType.KeyDown &&
                        event.nativeKeyEvent.keyCode == KeyEvent.KEYCODE_DPAD_UP -> {
                        onUp()
                        true
                    }
                    event.type == KeyEventType.KeyUp &&
                        event.nativeKeyEvent.keyCode == KeyEvent.KEYCODE_MENU -> {
                        onFavorite()
                        true
                    }
                    else -> false
                }
            }
            .background(surface, shape)
            .border(
                if (focused) 2.dp else 0.dp,
                if (focused) Select else Color.Transparent,
                shape
            )
            .semantics {
                contentDescription = app.title + if (favorite) " " + favoriteLabel else ""
            }
            .clickable(onClick = onOpen)
            .padding(horizontal = 8.dp, vertical = if (short) 7.dp else 11.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Box(Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
            OriginalAppIcon(app, Modifier.size(iconSize))
            if (favorite) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .size(if (short) 6.dp else 8.dp)
                        .background(Iris, CircleShape)
                )
            }
        }
        Text(
            text = app.title,
            color = if (focused) White else Silver,
            fontSize = if (short) 11.sp else 14.sp,
            fontWeight = if (focused) FontWeight.SemiBold else FontWeight.Medium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun SettingsStage(
    options: LauncherOptions,
    short: Boolean,
    modifier: Modifier,
    onChange: (LauncherOptions) -> Unit,
    onSystemSettings: () -> Unit,
    onClose: () -> Unit
) {
    val firstRequester = remember { FocusRequester() }
    LaunchedEffect(Unit) {
        withFrameNanos { }
        firstRequester.requestFocus()
    }
    Column(
        modifier = modifier
            .background(
                Brush.verticalGradient(
                    listOf(Color(0xFF111124), Color(0xFF080A13), Night)
                ),
                RoundedCornerShape(if (short) 20.dp else 27.dp)
            )
            .verticalScroll(rememberScrollState())
            .padding(horizontal = if (short) 22.dp else 40.dp, vertical = if (short) 15.dp else 27.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Text(
                    stringResource(R.string.launcher_settings),
                    color = White,
                    fontSize = if (short) 27.sp else 40.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = (-0.9).sp
                )
                Spacer(Modifier.height(5.dp))
                Text(
                    stringResource(R.string.settings_subtitle),
                    color = Silver,
                    fontSize = if (short) 12.sp else 14.sp
                )
            }
            Text(
                text = stringResource(R.string.settings_saved),
                color = Soft,
                fontSize = if (short) 10.sp else 12.sp,
                maxLines = 2
            )
        }
        Spacer(Modifier.height(if (short) 17.dp else 30.dp))
        SettingLine(
            title = stringResource(R.string.compact_grid),
            subtitle = stringResource(R.string.compact_grid_description),
            value = stringResource(if (options.compactGrid) R.string.enabled else R.string.disabled),
            short = short,
            requester = firstRequester,
            onClick = { onChange(options.copy(compactGrid = !options.compactGrid)) }
        )
        Spacer(Modifier.height(if (short) 7.dp else 14.dp))
        SettingLine(
            title = stringResource(R.string.favorites_first),
            subtitle = stringResource(R.string.favorites_first_description),
            value = stringResource(if (options.favoritesFirst) R.string.enabled else R.string.disabled),
            short = short,
            onClick = { onChange(options.copy(favoritesFirst = !options.favoritesFirst)) }
        )
        Spacer(Modifier.height(if (short) 7.dp else 14.dp))
        SettingLine(
            title = stringResource(R.string.android_settings),
            subtitle = stringResource(R.string.android_settings_description),
            value = "›",
            short = short,
            onClick = onSystemSettings
        )
        Spacer(Modifier.height(if (short) 8.dp else 16.dp))
        Text(
            text = stringResource(R.string.settings_saved),
            color = Soft,
            fontSize = 12.sp,
            modifier = Modifier.clickable(onClick = onClose)
        )
    }
}

@Composable
private fun SettingLine(
    title: String,
    subtitle: String,
    value: String,
    short: Boolean,
    requester: FocusRequester? = null,
    onClick: () -> Unit
) {
    var focused by remember { mutableStateOf(false) }
    val shape = RoundedCornerShape(14.dp)
    val fill by animateColorAsState(
        targetValue = if (focused) Color(0xFF242F64) else Color(0xFF161A2B),
        animationSpec = tween(155), label = "setting-line"
    )
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(if (short) 68.dp else 78.dp)
            .then(if (requester == null) Modifier else Modifier.focusRequester(requester))
            .onFocusChanged { focused = it.isFocused }
            .background(fill, shape)
            .border(
                if (focused) 2.dp else 0.dp,
                if (focused) Select else Color.Transparent, shape
            )
            .clickable(onClick = onClick)
            .padding(horizontal = if (short) 14.dp else 20.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Text(
                text = title,
                color = White,
                fontWeight = FontWeight.SemiBold,
                fontSize = if (short) 14.sp else 18.sp,
                maxLines = 1, overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = subtitle,
                color = Silver,
                fontSize = if (short) 11.sp else 13.sp,
                maxLines = 1, overflow = TextOverflow.Ellipsis
            )
        }
        Spacer(Modifier.width(15.dp))
        Text(
            text = value,
            color = if (focused) White else Iris,
            fontWeight = FontWeight.SemiBold,
            fontSize = if (short) 12.sp else 15.sp,
            maxLines = 1
        )
    }
}

@Composable
private fun DockMessage(message: String) {
    Box(
        Modifier.fillMaxWidth().height(100.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        Text(
            text = message,
            color = Silver,
            fontSize = 16.sp,
            lineHeight = 22.sp,
            maxLines = 3
        )
    }
}

private val TimeFormat = DateTimeFormatter.ofPattern("HH:mm")
private fun formattedTime() = LocalTime.now().format(TimeFormat)
