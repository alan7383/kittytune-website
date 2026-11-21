@file:OptIn(ExperimentalMaterial3ExpressiveApi::class)

package com.alananasss.kittytunewebsite.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.QueueMusic
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInParent
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.alananasss.kittytunewebsite.ui.demos.*
import com.alananasss.kittytunewebsite.ui.kit.*
import kittytunewebsite.generated.resources.*
import kotlinx.coroutines.launch

private val categoryIcons: Map<String, List<ImageVector>> = mapOf(
    "player" to listOf(Icons.Rounded.PlayCircle, Icons.AutoMirrored.Rounded.QueueMusic, Icons.Rounded.Speed, Icons.Rounded.Bedtime, Icons.Rounded.ViewInAr, Icons.Rounded.Shuffle),
    "lyrics" to listOf(Icons.Rounded.Lyrics, Icons.Rounded.Source, Icons.Rounded.Translate, Icons.Rounded.Abc, Icons.Rounded.People, Icons.Rounded.AudioFile),
    "sound" to listOf(Icons.Rounded.GraphicEq, Icons.Rounded.Tune, Icons.Rounded.VolumeUp, Icons.Rounded.CallSplit, Icons.Rounded.HighQuality, Icons.Rounded.Album),
    "soundcloud" to listOf(Icons.Rounded.Sync, Icons.Rounded.PlaylistAdd, Icons.Rounded.Forum, Icons.Rounded.AccountCircle, Icons.Rounded.PersonOutline, Icons.Rounded.SmartDisplay),
    "discover" to listOf(Icons.Rounded.Waves, Icons.Rounded.Mic, Icons.Rounded.VerifiedUser, Icons.Rounded.TrendingUp, Icons.Rounded.MoveToInbox, Icons.Rounded.Link),
    "library" to listOf(Icons.Rounded.DownloadForOffline, Icons.Rounded.Folder, Icons.Rounded.FolderSpecial, Icons.Rounded.Backup, Icons.Rounded.Storage, Icons.Rounded.DriveFileMove),
    "together" to listOf(Icons.Rounded.QrCode2, Icons.Rounded.Sync, Icons.AutoMirrored.Rounded.QueueMusic, Icons.Rounded.AddReaction, Icons.Rounded.Lock, Icons.Rounded.Celebration),
    "style" to listOf(Icons.Rounded.Palette, Icons.Rounded.Contrast, Icons.Rounded.TextFields, Icons.Rounded.Apps, Icons.Rounded.Movie, Icons.Rounded.Wallpaper),
    "fun" to listOf(Icons.Rounded.Pets, Icons.Rounded.Computer, Icons.Rounded.Badge, Icons.Rounded.AutoStories, Icons.Rounded.EmojiEvents, Icons.Rounded.Insights),
    "system" to listOf(Icons.Rounded.Tune, Icons.Rounded.Forum, Icons.Rounded.Watch, Icons.Rounded.DirectionsCar, Icons.Rounded.SettingsRemote, Icons.Rounded.SystemUpdate),
)

@Composable
fun FeaturesPage() {
    val s = site()
    val cs = MaterialTheme.colorScheme
    val state = androidx.compose.foundation.lazy.rememberLazyListState()
    val scope = rememberCoroutineScope()
    val insets = LocalChromeInsets.current
    val density = androidx.compose.ui.platform.LocalDensity.current
    val stickyOffsetPx = with(density) { 72.dp.roundToPx() }

    // Item 0 is the header, items 1..n the chapters, then the footer.
    val active by remember {
        derivedStateOf {
            val info = state.layoutInfo
            val probe = info.viewportStartOffset + stickyOffsetPx * 3
            val hit = info.visibleItemsInfo.lastOrNull { it.index in 1..s.categories.size && it.offset <= probe }
            s.categories[((hit?.index ?: 1) - 1).coerceIn(0, s.categories.lastIndex)].id
        }
    }
    val showSticky by remember { derivedStateOf { state.firstVisibleItemIndex >= 1 } }

    fun jump(id: String) {
        val i = s.categories.indexOfFirst { it.id == id }
        if (i < 0) return
        scope.launch { state.animateScrollToItem(i + 1, -stickyOffsetPx) }
    }

    Box(Modifier.fillMaxSize()) {
        LazyPageColumn(state) {
            item(key = "header") {
                ContentWidth {
                    val narrow = maxWidth < 640.dp
                    Column(Modifier.padding(top = 32.dp, bottom = 64.dp), verticalArrangement = Arrangement.spacedBy(28.dp)) {
                        Eyebrow(s.featuresEyebrow, Icons.Rounded.AutoAwesome, modifier = Modifier.reveal())
                        Text(
                            s.featuresTitle,
                            style = if (narrow) MaterialTheme.typography.displayMedium else MaterialTheme.typography.displayLarge.copy(fontSize = androidx.compose.ui.unit.TextUnit(84f, androidx.compose.ui.unit.TextUnitType.Sp), lineHeight = androidx.compose.ui.unit.TextUnit(88f, androidx.compose.ui.unit.TextUnitType.Sp)),
                            color = cs.onSurface,
                            modifier = Modifier.reveal(100, 80f)
                        )
                        Text(s.featuresSubtitle, style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.W500), color = cs.onSurfaceVariant, modifier = Modifier.reveal(200))
                        CategoryChips(s.categories, active, wrap = true, onPick = ::jump)
                    }
                }
            }
            items(s.categories.size, key = { s.categories[it].id }) { i ->
                Box(Modifier.padding(bottom = 96.dp)) { CategorySection(i, s.categories[i]) }
            }
            item(key = "footer") { SiteFooter() }
        }

        // sticky chapter bar
        AnimatedVisibility(
            visible = showSticky,
            enter = fadeIn() + slideInVertically { -it },
            exit = fadeOut() + slideOutVertically { -it },
            modifier = Modifier.align(Alignment.TopCenter).padding(top = insets.top).zIndex(40f)
        ) {
            Box(
                Modifier.padding(horizontal = 12.dp).shadow(10.dp, CircleShape).clip(CircleShape).background(cs.surfaceContainerHigh).padding(6.dp)
            ) {
                CategoryChips(s.categories, active, wrap = false, onPick = ::jump)
            }
        }
    }
}

@Composable
private fun CategoryChips(categories: List<CategoryText>, active: String, wrap: Boolean, onPick: (String) -> Unit) {
    val content: @Composable () -> Unit = {
        categories.forEachIndexed { i, cat ->
            ToggleButton(
                checked = cat.id == active,
                onCheckedChange = { onPick(cat.id) },
                modifier = Modifier.height(40.dp),
                contentPadding = PaddingValues(start = 12.dp, end = 16.dp),
            ) {
                Icon(chapterIcon(cat.id), null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text(cat.label, style = MaterialTheme.typography.labelLarge, maxLines = 1)
            }
        }
    }
    if (wrap) {
        Wrap(hSpacing = 8.dp, vSpacing = 8.dp) { content() }
    } else {
        Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) { content() }
    }
}

private fun chapterIcon(id: String): ImageVector = when (id) {
    "player" -> Icons.Rounded.PlayCircle
    "lyrics" -> Icons.Rounded.Lyrics
    "sound" -> Icons.Rounded.GraphicEq
    "soundcloud" -> Icons.Rounded.Cloud
    "discover" -> Icons.Rounded.Explore
    "library" -> Icons.Rounded.LibraryMusic
    "together" -> Icons.Rounded.Groups
    "style" -> Icons.Rounded.Palette
    "fun" -> Icons.Rounded.Pets
    else -> Icons.Rounded.DevicesOther
}

private val chapterShapes by lazy {
    listOf(
        MaterialShapes.Cookie9Sided, MaterialShapes.Sunny, MaterialShapes.Clover4Leaf, MaterialShapes.Puffy, MaterialShapes.Gem,
        MaterialShapes.SoftBurst, MaterialShapes.Flower, MaterialShapes.Cookie12Sided, MaterialShapes.Pill, MaterialShapes.Ghostish,
    )
}

/** One chapter: heading, then the feature list next to a live demo you can play with. */
@Composable
private fun CategorySection(index: Int, cat: CategoryText) {
    val s = site()
    val cs = MaterialTheme.colorScheme
    val icons = categoryIcons[cat.id] ?: emptyList()
    ContentWidth {
        val wide = maxWidth >= 1040.dp
        val narrow = maxWidth < 640.dp
        Column(verticalArrangement = Arrangement.spacedBy(28.dp)) {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp), modifier = Modifier.reveal()) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    ShapeIcon(chapterIcon(cat.id), chapterShapes[index % chapterShapes.size], 48.dp, cs.primaryContainer, cs.onPrimaryContainer)
                    Text(cat.label, style = MaterialTheme.typography.titleMedium, color = cs.primary)
                }
                Text(cat.title, style = if (narrow) MaterialTheme.typography.headlineLarge else MaterialTheme.typography.displaySmall, color = cs.onSurface)
                Text(cat.subtitle, style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.W500), color = cs.onSurfaceVariant, modifier = Modifier.widthIn(max = 720.dp))
            }
            val list: @Composable (Modifier) -> Unit = { m ->
                FeatureList(cat.items, icons, m.reveal(80))
            }
            val demo: @Composable (Modifier) -> Unit = { m ->
                ViewportGate(m.reveal(160, 60f)) { CategoryDemo(cat.id) }
            }
            if (wide) {
                Row(horizontalArrangement = Arrangement.spacedBy(24.dp), verticalAlignment = Alignment.Top) {
                    list(Modifier.weight(1f))
                    demo(Modifier.weight(1.1f))
                }
            } else {
                list(Modifier.fillMaxWidth())
                demo(Modifier.fillMaxWidth())
            }
        }
    }
}

/**
 * Material 3 Expressive grouped list (Android 16 settings style): one block whose items are separated
 * by a hairline gap, large corners only on the outside of the group.
 */
@Composable
private fun FeatureList(items: List<FeatureText>, icons: List<ImageVector>, modifier: Modifier) {
    val cs = MaterialTheme.colorScheme
    Column(modifier, verticalArrangement = Arrangement.spacedBy(3.dp)) {
        items.forEachIndexed { i, item ->
            val big = 28.dp
            val small = 6.dp
            val shape = when {
                items.size == 1 -> RoundedCornerShape(big)
                i == 0 -> RoundedCornerShape(topStart = big, topEnd = big, bottomStart = small, bottomEnd = small)
                i == items.lastIndex -> RoundedCornerShape(topStart = small, topEnd = small, bottomStart = big, bottomEnd = big)
                else -> RoundedCornerShape(small)
            }
            Row(
                Modifier.fillMaxWidth().clip(shape).background(cs.surfaceContainerLow).padding(horizontal = 18.dp, vertical = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    Modifier.size(44.dp).clip(CircleShape).background(cs.secondaryContainer),
                    contentAlignment = Alignment.Center
                ) { Icon(icons.getOrElse(i) { Icons.Rounded.Star }, null, tint = cs.onSecondaryContainer, modifier = Modifier.size(22.dp)) }
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(item.title, style = MaterialTheme.typography.titleMedium, color = cs.onSurface)
                    Text(item.desc, style = MaterialTheme.typography.bodyMedium, color = cs.onSurfaceVariant)
                }
            }
        }
    }
}

@Composable
private fun DemoStage(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    val cs = MaterialTheme.colorScheme
    Column(
        modifier.fillMaxWidth().clip(RoundedCornerShape(36.dp)).background(cs.surfaceContainerLow).padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(18.dp),
        content = content
    )
}

/** A small "try it" hint so it is obvious the panel is live, not a picture. */
@Composable
private fun TryHint(text: String) {
    val cs = MaterialTheme.colorScheme
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Icon(Icons.Rounded.TouchApp, null, tint = cs.primary, modifier = Modifier.size(18.dp))
        Text(text, style = MaterialTheme.typography.labelLarge, color = cs.onSurfaceVariant)
    }
}

@Composable
private fun CategoryDemo(id: String) {
    val s = site()
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        when (id) {
            "player" -> DemoStage {
                // A real capture from the Pixel 10 emulator, not a mock-up.
                Screenshot(Res.drawable.android_player_live, "KittyTune · sekairotten", Modifier.height(640.dp))
            }
            "lyrics" -> {
                DemoStage { ScaleToFit(860.dp, Modifier.fillMaxWidth()) { DesktopLyricsMock() } }
                DemoStage { TryHint(s.tryTranslate); LyricsTranslateDemo(Modifier.fillMaxWidth()) }
            }
            "sound" -> {
                DemoStage { TryHint(s.tryFx); AudioFxDemo(Modifier.fillMaxWidth()) }
                DemoStage { TryHint(s.tryStems); StemsDemo(Modifier.fillMaxWidth()) }
            }
            "soundcloud" -> DemoStage { SyncDemo(Modifier.fillMaxWidth()) }
            "discover" -> {
                DemoStage { TryHint(s.tryWave); MyWaveDemo(Modifier.fillMaxWidth()) }
                DemoStage { ShazamDemo(Modifier.fillMaxWidth(), height = 340.dp) }
            }
            "library" -> DemoStage { TryHint(s.tryOffline); OfflineDemo(Modifier.fillMaxWidth()) }
            "together" -> {
                DemoStage { TryHint(s.tryTogether); TogetherDemo(Modifier.fillMaxWidth()) }
                DemoStage { TryHint(s.tryConnect); ConnectDemo(Modifier.fillMaxWidth()) }
            }
            "style" -> {
                DemoStage { DynamicColorDemo(Modifier.fillMaxWidth()) }
                DemoStage {
                    Row(horizontalArrangement = Arrangement.spacedBy(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Screenshot(Res.drawable.android_amoled, "AMOLED", Modifier.height(360.dp))
                        Screenshot(Res.drawable.android_iconchanger, "App icons", Modifier.height(360.dp))
                    }
                }
            }
            "fun" -> {
                DemoStage { TryHint(s.tryPet); PetDemo(Modifier.fillMaxWidth()) }
                DemoStage { AchievementsDemo(Modifier.fillMaxWidth()) }
            }
            else -> DemoStage {
                Row(horizontalArrangement = Arrangement.spacedBy(20.dp), verticalAlignment = Alignment.CenterVertically) {
                    WearDemo(size = 170.dp)
                    DiscordDemo(Modifier.weight(1f))
                }
            }
        }
    }
}
