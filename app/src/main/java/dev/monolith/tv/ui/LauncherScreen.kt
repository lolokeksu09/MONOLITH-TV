package dev.monolith.tv.ui

import android.view.KeyEvent
import android.widget.ImageView
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.viewinterop.AndroidView
import androidx.tv.material3.Text
import dev.monolith.tv.R
import dev.monolith.tv.catalog.AppOrder
import dev.monolith.tv.catalog.TvApp
import kotlinx.coroutines.delay
import java.time.LocalTime
import java.time.format.DateTimeFormatter

private val Background = Color(0xFF0B0D10)
private val TileBackground = Color(0xFF171A1E)
private val TileFocused = Color(0xFF262D33)
private val Primary = Color(0xFFF3F4F5)
private val Secondary = Color(0xFFABB1B9)

@Composable
fun LauncherScreen(
    apps: List<TvApp>,
    favoritePackages: Set<String>,
    loading: Boolean,
    onLaunch: (TvApp) -> Unit,
    onToggleFavorite: (String) -> Unit,
    onOpenSettings: () -> Unit
) {
    val context = LocalContext.current
    val ordered = remember(apps, favoritePackages) {
        AppOrder.sort(apps, favoritePackages, { it.packageName }, { it.title })
    }
    val clock by produceState(initialValue = formattedTime()) {
        while (true) {
            value = formattedTime()
            delay(15_000)
        }
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
                Text("MONOLITH TV", color = Primary, fontSize = 28.sp, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(5.dp))
                Text(
                    context.getString(R.string.remote_hint),
                    color = Secondary,
                    fontSize = 14.sp
                )
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(clock, color = Primary, fontSize = 25.sp)
                Spacer(Modifier.width(26.dp))
                ActionTile(context.getString(R.string.settings), onOpenSettings)
            }
        }

        Spacer(Modifier.height(28.dp))
        when {
            loading -> Text(context.getString(R.string.loading), color = Secondary, fontSize = 18.sp)
            ordered.isEmpty() -> Text(context.getString(R.string.no_apps), color = Primary, fontSize = 18.sp)
            else -> LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = 170.dp),
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 20.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(ordered, key = { it.component.flattenToString() }) { app ->
                    AppTile(
                        app = app,
                        isFavorite = app.packageName in favoritePackages,
                        onLaunch = { onLaunch(app) },
                        onToggleFavorite = { onToggleFavorite(app.packageName) }
                    )
                }
            }
        }
    }
}

@Composable
private fun AppTile(
    app: TvApp,
    isFavorite: Boolean,
    onLaunch: () -> Unit,
    onToggleFavorite: () -> Unit
) {
    var focused by remember { mutableStateOf(false) }
    val shape = RoundedCornerShape(12.dp)
    val context = LocalContext.current

    Column(
        modifier = Modifier
            .height(146.dp)
            .onFocusChanged { focused = it.isFocused }
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
                color = if (focused) Primary else Color(0xFF393D43),
                shape = shape
            )
            .background(if (focused) TileFocused else TileBackground, shape)
            .semantics {
                contentDescription = app.title + if (isFavorite) " " + context.getString(R.string.favorite) else ""
            }
            .clickable(onClick = onLaunch)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        horizontalAlignment = Alignment.Start,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        // The original app icon is drawn unmodified by an Android ImageView.
        AndroidView(
            factory = { viewContext ->
                ImageView(viewContext).apply {
                    scaleType = ImageView.ScaleType.FIT_CENTER
                    adjustViewBounds = true
                }
            },
            update = { view ->
                view.setImageDrawable(app.originalIcon.constantState?.newDrawable()?.mutate() ?: app.originalIcon)
            },
            modifier = Modifier.size(60.dp)
        )
        Column {
            Text(app.title, color = Primary, fontSize = 16.sp, maxLines = 1)
            Text(
                if (isFavorite) context.getString(R.string.favorite) else " ",
                color = Secondary,
                fontSize = 12.sp,
                maxLines = 1
            )
        }
    }
}

@Composable
private fun ActionTile(title: String, onClick: () -> Unit) {
    var focused by remember { mutableStateOf(false) }
    val shape = RoundedCornerShape(10.dp)
    Box(
        modifier = Modifier
            .onFocusChanged { focused = it.isFocused }
            .border(if (focused) 2.dp else 1.dp, if (focused) Primary else Secondary, shape)
            .background(if (focused) TileFocused else TileBackground, shape)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(title, color = Primary, fontSize = 16.sp)
    }
}

private val TimeFormat = DateTimeFormatter.ofPattern("HH:mm")
private fun formattedTime(): String = LocalTime.now().format(TimeFormat)

