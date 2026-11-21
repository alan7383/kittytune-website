@file:OptIn(ExperimentalMaterial3ExpressiveApi::class)

package com.alananasss.kittytunewebsite.ui.demos

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.alananasss.kittytunewebsite.ui.LocalThemeController
import com.alananasss.kittytunewebsite.ui.colorPresets
import com.alananasss.kittytunewebsite.ui.kit.MorphingBlob
import com.alananasss.kittytunewebsite.ui.kit.PolygonShape
import com.alananasss.kittytunewebsite.ui.kit.ShapeBox
import com.alananasss.kittytunewebsite.ui.kit.Wrap
import com.alananasss.kittytunewebsite.ui.kit.rememberClock
import com.alananasss.kittytunewebsite.ui.kit.springyPress
import com.alananasss.kittytunewebsite.ui.kit.wave
import com.alananasss.kittytunewebsite.ui.pet.PetMood
import com.alananasss.kittytunewebsite.ui.pet.PixelPet
import com.alananasss.kittytunewebsite.ui.site
import kotlinx.coroutines.delay
import kittytunewebsite.generated.resources.Res
import kittytunewebsite.generated.resources.cover_eternal_addict
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

private val listenerInitials = listOf("A", "M", "J", "S", "K")

/** The track the demo room is listening to. */
private val TogetherTrack get() = DemoTracks[3]
private val TogetherTrackTitle get() = TogetherTrack.title
private val TogetherTrackArtist get() = TogetherTrack.artist
private val TogetherTrackSeconds get() = TogetherTrack.durationSec.toFloat()

/** Together room: who's listening, the shared track in sync on every device, tap a face to react. */
@Composable
fun TogetherDemo(modifier: Modifier = Modifier) {
    val s = site()
    val cs = MaterialTheme.colorScheme
    val clock = rememberClock()
    val avatarColors = listOf(cs.primary, cs.tertiary, cs.secondary, cs.primaryContainer, cs.tertiaryContainer)
    val avatarInk = listOf(cs.onPrimary, cs.onTertiary, cs.onSecondary, cs.onPrimaryContainer, cs.onTertiaryContainer)
    var reactions by remember { mutableStateOf(listOf<Pair<Int, Float>>()) }
    val reactionIcons = listOf(Icons.Rounded.Favorite, Icons.Rounded.LocalFireDepartment, Icons.Rounded.Headphones, Icons.Rounded.AutoAwesome, Icons.Rounded.ThumbUp)
    Column(modifier, verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Surface(shape = CircleShape, color = cs.primaryContainer) {
                Row(Modifier.padding(horizontal = 14.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Rounded.Lock, null, tint = cs.onPrimaryContainer, modifier = Modifier.size(16.dp))
                    Text("${s.demoRoom} KT7F-29QX", style = MaterialTheme.typography.labelLarge, color = cs.onPrimaryContainer)
                }
            }
            Box(Modifier.size(8.dp).clip(CircleShape).background(Color(0xFF3DDC84)).graphicsLayer { alpha = 0.55f + 0.45f * wave(clock.value, 1.6f) })
            Text("${listenerInitials.size} ${s.demoListenersSuffix}", style = MaterialTheme.typography.labelLarge, color = cs.onSurfaceVariant)
        }
        // avatar stack
        Box(Modifier.fillMaxWidth().height(86.dp)) {
            Row(Modifier.align(Alignment.BottomStart), horizontalArrangement = Arrangement.spacedBy((-12).dp)) {
                listenerInitials.forEachIndexed { i, l ->
                    val source = remember { MutableInteractionSource() }
                    Box(
                        Modifier.size(52.dp).springyPress(source, 0.88f, 1.08f)
                            .clip(CircleShape).background(cs.surfaceContainerLow).padding(3.dp)
                            .clip(CircleShape).background(avatarColors[i])
                            .clickable(source, null) { reactions = (reactions + (i to clock.value)).takeLast(10) },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(l, style = MaterialTheme.typography.titleMedium, color = avatarInk[i])
                    }
                }
            }
            reactions.forEach { (who, at) ->
                // Age is read in the layer block: floating reactions animate without recomposing the card.
                Icon(
                    reactionIcons[who % reactionIcons.size], null, tint = avatarColors[who],
                    modifier = Modifier.align(Alignment.BottomStart).offset(x = (who * 40 + 14).dp, y = (-44).dp).size(24.dp)
                        .graphicsLayer {
                            val age = clock.value - at
                            val live = age in 0f..1.8f
                            translationY = -age * 40f
                            alpha = if (live) 1f - age / 1.8f else 0f
                            val sc = 0.7f + age.coerceIn(0f, 1.8f) * 0.4f; scaleX = sc; scaleY = sc
                        }
                )
            }
        }
        // shared track, in sync on every device
        Row(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(cs.surfaceContainerHighest).padding(10.dp),
            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            androidx.compose.foundation.Image(
                org.jetbrains.compose.resources.painterResource(Res.drawable.cover_eternal_addict), TogetherTrackTitle,
                contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                modifier = Modifier.size(52.dp).clip(RoundedCornerShape(14.dp))
            )
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(TogetherTrackTitle, style = MaterialTheme.typography.titleSmall, color = cs.onSurface, maxLines = 1)
                Text(TogetherTrackArtist, style = MaterialTheme.typography.labelMedium, color = cs.onSurfaceVariant, maxLines = 1)
                Spacer(Modifier.height(4.dp))
                LinearWavyProgressIndicator(progress = { (clock.value / TogetherTrackSeconds + 0.18f) % 1f }, modifier = Modifier.fillMaxWidth(), color = cs.primary, trackColor = cs.surfaceContainer)
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            listOf(Icons.Rounded.PhoneAndroid to "Android", Icons.Rounded.DesktopWindows to "Windows", Icons.Rounded.Computer to "Linux").forEach { (ic, name) ->
                Surface(shape = CircleShape, color = cs.secondaryContainer) {
                    Row(Modifier.padding(horizontal = 10.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Icon(ic, null, tint = cs.onSecondaryContainer, modifier = Modifier.size(14.dp))
                        Text(name, style = MaterialTheme.typography.labelMedium, color = cs.onSecondaryContainer)
                        Icon(Icons.Rounded.Check, null, tint = cs.onSecondaryContainer, modifier = Modifier.size(14.dp))
                    }
                }
            }
        }
    }
}

/** Seed swatches that re-theme the entire site, plus the resulting tonal palette. */
@Composable
fun DynamicColorDemo(modifier: Modifier = Modifier) {
    val s = site()
    val cs = MaterialTheme.colorScheme
    val controller = LocalThemeController.current
    Column(modifier, verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(s.demoSeedHint, style = MaterialTheme.typography.labelLarge, color = cs.onSurfaceVariant)
        Wrap(hSpacing = 10.dp, vSpacing = 10.dp) {
            colorPresets.forEachIndexed { i, preset ->
                val selected = controller?.selectedColor?.name == preset.name
                val rot by animateFloatAsState(if (selected) 45f else 0f, spring(0.4f, 200f))
                val scale by animateFloatAsState(if (selected) 1.12f else 1f, spring(0.4f, 300f))
                val polygon = if (selected) MaterialShapes.Cookie9Sided else MaterialShapes.Circle
                val source = remember { MutableInteractionSource() }
                Box(
                    Modifier.size(44.dp).graphicsLayer { scaleX = scale; scaleY = scale }.springyPress(source, 0.85f, 1.1f)
                        .clip(PolygonShape(polygon, rot)).background(preset.color)
                        .clickable(source, null) { controller?.selectColor?.invoke(preset) },
                    contentAlignment = Alignment.Center
                ) {
                    if (selected) Icon(Icons.Rounded.Check, null, tint = Color.White, modifier = Modifier.size(20.dp))
                }
            }
        }
        Text(s.demoPaletteLabel, style = MaterialTheme.typography.labelLarge, color = cs.onSurfaceVariant)
        // The result, not a picker: each role as its colour and its container, labelled.
        val roles = listOf(
            Triple("Primary", cs.primary to cs.onPrimary, cs.primaryContainer to cs.onPrimaryContainer),
            Triple("Secondary", cs.secondary to cs.onSecondary, cs.secondaryContainer to cs.onSecondaryContainer),
            Triple("Tertiary", cs.tertiary to cs.onTertiary, cs.tertiaryContainer to cs.onTertiaryContainer),
        )
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            roles.forEach { (name, main, container) ->
                Column(Modifier.weight(1f).clip(RoundedCornerShape(18.dp))) {
                    Box(Modifier.fillMaxWidth().height(40.dp).background(main.first).padding(horizontal = 10.dp), contentAlignment = Alignment.CenterStart) {
                        Text(name, style = MaterialTheme.typography.labelMedium, color = main.second, maxLines = 1)
                    }
                    Box(Modifier.fillMaxWidth().height(28.dp).background(container.first).padding(horizontal = 10.dp), contentAlignment = Alignment.CenterStart) {
                        Text("Container", style = MaterialTheme.typography.labelSmall, color = container.second, maxLines = 1)
                    }
                }
            }
        }
    }
}

/** Kitty Pet: the real sprite sheets, re-rendered pixel by pixel, with a mood switcher. */
@Composable
fun PetDemo(modifier: Modifier = Modifier, compact: Boolean = false) {
    val s = site()
    val cs = MaterialTheme.colorScheme
    val moods = listOf(PetMood.Dj, PetMood.Hello, PetMood.Love, PetMood.Headbang, PetMood.Disco, PetMood.Loaf)
    var mood by remember { mutableIntStateOf(0) }
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Box(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(28.dp)).background(cs.surfaceContainerHighest).clickable { mood = (mood + 1) % moods.size }.padding(vertical = 8.dp),
            contentAlignment = Alignment.Center
        ) {
            AnimatedContent(mood, transitionSpec = { (fadeIn() + scaleIn(initialScale = 0.9f)) togetherWith (fadeOut() + scaleOut(targetScale = 1.05f)) }, label = "pet") { m ->
                PixelPet(moods[m], Modifier.fillMaxWidth(if (compact) 0.8f else 0.7f))
            }
        }
        Wrap(hSpacing = 6.dp, vSpacing = 6.dp, center = true) {
            moods.forEachIndexed { i, _ ->
                ToggleButton(
                    checked = mood == i,
                    onCheckedChange = { mood = i },
                    contentPadding = PaddingValues(horizontal = 12.dp),
                    modifier = Modifier.height(36.dp),
                ) { Text(s.demoMoods.getOrElse(i) { "" }, style = MaterialTheme.typography.labelMedium) }
            }
        }
    }
}

/** SoundCloud two-way sync: phone ⇄ cloud ⇄ desktop with packets flowing both ways. */
@Composable
fun SyncDemo(modifier: Modifier = Modifier) {
    val s = site()
    val cs = MaterialTheme.colorScheme
    val clock = rememberClock()
    Column(modifier, verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Box(Modifier.fillMaxWidth().height(110.dp)) {
            Canvas(Modifier.matchParentSize()) {
                val y = size.height / 2f
                val l = size.width * 0.14f
                val r = size.width * 0.86f
                drawLine(cs.outlineVariant, Offset(l, y), Offset(r, y), strokeWidth = 4f, cap = StrokeCap.Round)
                for (k in 0 until 4) {
                    val p = (clock.value * 0.35f + k / 4f) % 1f
                    val dir = k % 2 == 0
                    val x = if (dir) l + (r - l) * p else r - (r - l) * p
                    drawCircle(if (dir) cs.primary else cs.tertiary, radius = 9f, center = Offset(x, y + if (dir) -10f else 10f))
                }
            }
            SyncNode(Icons.Rounded.PhoneAndroid, Modifier.align(Alignment.CenterStart), cs.secondaryContainer, cs.onSecondaryContainer)
            SyncNode(Icons.Rounded.Cloud, Modifier.align(Alignment.Center), cs.primary, cs.onPrimary, big = true)
            SyncNode(Icons.Rounded.Computer, Modifier.align(Alignment.CenterEnd), cs.tertiaryContainer, cs.onTertiaryContainer)
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            // derivedStateOf: the counters recompose when a number changes, not on every frame.
            val likes by remember { derivedStateOf { 1284 + (clock.value / 1.3f).toInt() } }
            val lists by remember { derivedStateOf { 37 + (clock.value / 9f).toInt() } }
            val history by remember { derivedStateOf { 9021 + (clock.value / 0.8f).toInt() } }
            Counter(Icons.Rounded.Favorite, s.demoLikes, likes, Modifier.weight(1f))
            Counter(Icons.Rounded.LibraryMusic, s.demoPlaylists, lists, Modifier.weight(1f))
            Counter(Icons.Rounded.History, s.demoHistory, history, Modifier.weight(1f))
        }
    }
}

@Composable
private fun SyncNode(icon: androidx.compose.ui.graphics.vector.ImageVector, modifier: Modifier, bg: Color, fg: Color, big: Boolean = false) {
    ShapeBox(if (big) MaterialShapes.Cookie9Sided else MaterialShapes.Square, modifier.size(if (big) 72.dp else 52.dp).background(bg)) {
        Icon(icon, null, tint = fg, modifier = Modifier.size(if (big) 32.dp else 24.dp))
    }
}

@Composable
private fun Counter(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, value: Int, modifier: Modifier) {
    val cs = MaterialTheme.colorScheme
    Column(modifier.clip(RoundedCornerShape(18.dp)).background(cs.surfaceContainerHighest).padding(12.dp)) {
        Icon(icon, null, tint = cs.primary, modifier = Modifier.size(18.dp))
        AnimatedContent(value, transitionSpec = { slideInVertically { it } + fadeIn() togetherWith fadeOut() }, label = "count") { v ->
            Text(formatThousands(v), style = MaterialTheme.typography.titleMedium, color = cs.onSurface)
        }
        Text(label, style = MaterialTheme.typography.labelSmall, color = cs.onSurfaceVariant, maxLines = 1)
    }
}

private fun formatThousands(v: Int): String {
    val str = v.toString()
    val sb = StringBuilder()
    str.forEachIndexed { i, c ->
        if (i > 0 && (str.length - i) % 3 == 0) sb.append(',')
        sb.append(c)
    }
    return sb.toString()
}

/** Wear OS player on a round watch face with a wavy progress ring. */
@Composable
fun WearDemo(modifier: Modifier = Modifier, size: Dp = 200.dp) {
    val cs = MaterialTheme.colorScheme
    var playing by remember { mutableStateOf(true) }
    val pos = rememberPlayhead(playing, start = 30f, duration = 199f)
    val track = DemoTracks[3]
    WatchFrame(modifier, size = size) {
        CircularWavyProgressIndicator(
            progress = { pos.value / 199f },
            modifier = Modifier.fillMaxSize().padding(6.dp),
            color = cs.primary,
            trackColor = cs.surfaceContainerHighest,
        )
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(track.title, style = MaterialTheme.typography.labelLarge, color = Color.White, maxLines = 1)
            Text(track.artist, style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = 0.6f), maxLines = 1)
            Spacer(Modifier.height(6.dp))
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Rounded.SkipPrevious, null, tint = Color.White, modifier = Modifier.size(20.dp))
                val source = remember { MutableInteractionSource() }
                Box(
                    Modifier.size(48.dp).springyPress(source, 0.85f, 1.08f).clip(if (playing) RoundedCornerShape(14.dp) else CircleShape).background(cs.primary).clickable(source, null) { playing = !playing },
                    contentAlignment = Alignment.Center
                ) { Icon(if (playing) Icons.Rounded.Pause else Icons.Rounded.PlayArrow, null, tint = cs.onPrimary) }
                Icon(Icons.Rounded.SkipNext, null, tint = Color.White, modifier = Modifier.size(20.dp))
            }
            Spacer(Modifier.height(4.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                repeat(5) { i -> Box(Modifier.size(width = 10.dp, height = 3.dp).clip(CircleShape).background(if (i < 3) cs.primary else Color.White.copy(alpha = 0.2f))) }
            }
        }
    }
}

/** Discord Rich Presence card mock. */
@Composable
fun DiscordDemo(modifier: Modifier = Modifier) {
    val clock = rememberClock()
    val track = DemoTracks[5]
    val bg = Color(0xFF232428)
    Column(
        modifier.clip(RoundedCornerShape(20.dp)).background(bg).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text("LISTENING TO KITTYTUNE", style = MaterialTheme.typography.labelSmall, color = Color(0xFFB5BAC1), letterSpacing = 1.sp)
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Box {
                ArtCover(track.art, Modifier.size(64.dp), RoundedCornerShape(10.dp), animated = true)
                Box(Modifier.align(Alignment.BottomEnd).offset(4.dp, 4.dp).size(22.dp).clip(CircleShape).background(bg).padding(3.dp)) {
                    com.alananasss.kittytunewebsite.ui.kit.KittyLogo(Modifier.fillMaxSize().clip(CircleShape).background(Color(0xFF1A6DFF)).padding(2.dp), tint = Color.White)
                }
            }
            Column(Modifier.weight(1f)) {
                Text(track.title, style = MaterialTheme.typography.titleSmall, color = Color.White, maxLines = 1)
                Text("by ${track.artist}", style = MaterialTheme.typography.bodySmall, color = Color(0xFFDBDEE1), maxLines = 1)
                Spacer(Modifier.height(6.dp))
                val progress = { (clock.value / track.durationSec + 0.3f) % 1f }
                val elapsed by remember(track) { derivedStateOf { (progress() * track.durationSec).toInt() } }
                Box(
                    Modifier.fillMaxWidth().height(4.dp).clip(CircleShape).background(Color(0xFF4E5058)).drawBehind {
                        drawRoundRect(Color.White, size = size.copy(width = size.width * progress()), cornerRadius = CornerRadius(size.height / 2f))
                    }
                )
                Row(Modifier.fillMaxWidth().padding(top = 2.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(formatTime(elapsed), fontSize = 10.sp, color = Color(0xFFB5BAC1))
                    Text(formatTime(track.durationSec), fontSize = 10.sp, color = Color(0xFFB5BAC1))
                }
            }
        }
        Box(Modifier.fillMaxWidth().height(34.dp).clip(RoundedCornerShape(8.dp)).background(Color(0xFF4E5058)), contentAlignment = Alignment.Center) {
            Text("Join Together room", style = MaterialTheme.typography.labelLarge, color = Color.White)
        }
    }
}

/** Achievement badges that unlock in turn. */
@Composable
fun AchievementsDemo(modifier: Modifier = Modifier) {
    val cs = MaterialTheme.colorScheme
    val clock = rememberClock()
    val shapes = listOf(MaterialShapes.Cookie9Sided, MaterialShapes.Sunny, MaterialShapes.Gem, MaterialShapes.Flower, MaterialShapes.SoftBurst, MaterialShapes.Clover8Leaf, MaterialShapes.Puffy, MaterialShapes.Burst)
    val icons = listOf(Icons.Rounded.Headphones, Icons.Rounded.LocalFireDepartment, Icons.Rounded.Favorite, Icons.Rounded.NightsStay, Icons.Rounded.Explore, Icons.Rounded.Lyrics, Icons.Rounded.Groups, Icons.Rounded.EmojiEvents)
    val unlocked by remember { derivedStateOf { ((clock.value / 1.2f).toInt() % (shapes.size + 3)).coerceAtMost(shapes.size) } }
    Wrap(modifier, hSpacing = 10.dp, vSpacing = 10.dp) {
        shapes.forEachIndexed { i, sh ->
            val on = i < unlocked
            val sc by animateFloatAsState(if (on) 1f else 0.82f, spring(0.35f, 300f))
            val bg by animateColorAsState(if (on) (if (i % 2 == 0) cs.primary else cs.tertiary) else cs.surfaceContainerHighest)
            ShapeBox(sh, Modifier.size(52.dp).graphicsLayer { scaleX = sc; scaleY = sc }.background(bg), rotation = i * 11f) {
                Icon(if (on) icons[i] else Icons.Rounded.Lock, null, tint = if (on) (if (i % 2 == 0) cs.onPrimary else cs.onTertiary) else cs.onSurfaceVariant, modifier = Modifier.size(22.dp))
            }
        }
    }
}
