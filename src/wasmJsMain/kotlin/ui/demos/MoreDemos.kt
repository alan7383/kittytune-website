@file:OptIn(ExperimentalMaterial3ExpressiveApi::class)

package com.alananasss.kittytunewebsite.ui.demos

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.alananasss.kittytunewebsite.ui.kit.MorphingBlob
import com.alananasss.kittytunewebsite.ui.kit.rememberClock
import com.alananasss.kittytunewebsite.ui.kit.springyPress
import com.alananasss.kittytunewebsite.ui.site
import kotlinx.coroutines.launch

/** Connected M3 Expressive button group: equal segments, the checked one rounds fully. */
@Composable
fun SegmentedGroup(
    labels: List<String>,
    selected: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
    icons: List<ImageVector?> = emptyList(),
    height: androidx.compose.ui.unit.Dp = 44.dp,
) {
    Row(modifier, horizontalArrangement = Arrangement.spacedBy(ButtonGroupDefaults.ConnectedSpaceBetween)) {
        labels.forEachIndexed { i, label ->
            ToggleButton(
                checked = selected == i,
                onCheckedChange = { onSelect(i) },
                shapes = when (i) {
                    0 -> ButtonGroupDefaults.connectedLeadingButtonShapes()
                    labels.lastIndex -> ButtonGroupDefaults.connectedTrailingButtonShapes()
                    else -> ButtonGroupDefaults.connectedMiddleButtonShapes()
                },
                modifier = Modifier.weight(1f).height(height),
                contentPadding = PaddingValues(horizontal = 8.dp),
            ) {
                icons.getOrNull(i)?.let {
                    Icon(it, null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                }
                Text(label, style = MaterialTheme.typography.labelLarge, maxLines = 1)
            }
        }
    }
}

private data class TriLine(val original: String, val romanized: String)

private val cyrillicLines = listOf(
    TriLine("Танцуем до рассвета", "Tantsuyem do rassveta"),
    TriLine("Я слышу твой голос", "Ya slyshu tvoy golos"),
    TriLine("Звёзды ещё горят", "Zvyozdy yeshchyo goryat"),
)

/** One line, three ways: switch between the original, the romanization and your language. */
@Composable
fun LyricsTranslateDemo(modifier: Modifier = Modifier) {
    val s = site()
    val cs = MaterialTheme.colorScheme
    val clock = rememberClock()
    var mode by remember { mutableIntStateOf(2) }
    val activeLine by remember { derivedStateOf { ((clock.value / 2.6f).toInt()) % cyrillicLines.size } }
    Column(modifier, verticalArrangement = Arrangement.spacedBy(14.dp)) {
        SegmentedGroup(listOf(s.demoOriginal, s.demoRomanized, s.demoTranslated), mode, { mode = it }, Modifier.fillMaxWidth())
        Column(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(24.dp)).background(cs.surfaceContainerHighest).padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            cyrillicLines.forEachIndexed { i, l ->
                val on = i == activeLine
                val alpha by animateFloatAsState(if (on) 1f else 0.38f, tween(400))
                val scale by animateFloatAsState(if (on) 1f else 0.96f, spring(0.7f, 300f))
                Column(Modifier.graphicsLayer { this.alpha = alpha; scaleX = scale; scaleY = scale; transformOrigin = androidx.compose.ui.graphics.TransformOrigin(0f, 0.5f) }) {
                    Text(
                        if (mode == 1) l.romanized else l.original,
                        style = MaterialTheme.typography.titleLarge, color = cs.onSurface, maxLines = 1
                    )
                    AnimatedContent(mode, transitionSpec = { (fadeIn() + slideInVertically { it / 2 }) togetherWith fadeOut() }, label = "sub") { m ->
                        val sub = when (m) {
                            0 -> null
                            1 -> l.original
                            else -> s.demoTranslatedLines.getOrNull(i)
                        }
                        if (sub != null) Text(sub, style = MaterialTheme.typography.bodyMedium, color = cs.primary, maxLines = 1)
                    }
                }
            }
        }
    }
}

/** Phone ⇄ desktop: tap a device and the session moves there, position and all. */
@Composable
fun ConnectDemo(modifier: Modifier = Modifier) {
    val s = site()
    val cs = MaterialTheme.colorScheme
    var target by remember { mutableIntStateOf(1) }
    var playing by remember { mutableStateOf(true) }
    val pos = rememberPlayhead(playing, start = 64f, duration = 199f)
    val track = DemoTracks[3]
    val devices = listOf(Icons.Rounded.PhoneAndroid to "Pixel 10", Icons.Rounded.DesktopWindows to "Desktop", Icons.Rounded.Watch to "Wear OS")
    Column(modifier, verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            devices.forEachIndexed { i, (icon, name) ->
                val on = i == target
                val bg by animateColorAsState(if (on) cs.primary else cs.surfaceContainerHighest)
                val fg by animateColorAsState(if (on) cs.onPrimary else cs.onSurfaceVariant)
                val corner by androidx.compose.animation.core.animateDpAsState(if (on) 32.dp else 18.dp, spring(0.6f, 300f))
                val source = remember { MutableInteractionSource() }
                Column(
                    Modifier.weight(1f).springyPress(source, 0.94f, 1.03f).clip(RoundedCornerShape(corner)).background(bg)
                        .clickable(source, null) { target = i }.padding(vertical = 18.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(icon, null, tint = fg, modifier = Modifier.size(30.dp))
                    Text(name, style = MaterialTheme.typography.labelLarge, color = fg, maxLines = 1)
                    if (on) {
                        val clock = rememberClock()
                        // Equalizer bars drawn in the draw phase: no per-frame recomposition.
                        androidx.compose.foundation.Canvas(Modifier.size(width = 21.dp, height = 14.dp)) {
                            val bw = 3.dp.toPx()
                            val gap = (size.width - bw * 4) / 3f
                            repeat(4) { b ->
                                val h = if (playing) 0.35f + 0.65f * ((com.alananasss.kittytunewebsite.ui.kit.wave(clock.value, 0.6f + b * 0.13f, b * 0.21f) + 1f) / 2f) else 0.3f
                                val bh = size.height * h
                                drawRoundRect(fg, topLeft = androidx.compose.ui.geometry.Offset(b * (bw + gap), size.height - bh), size = androidx.compose.ui.geometry.Size(bw, bh), cornerRadius = androidx.compose.ui.geometry.CornerRadius(bw / 2f))
                            }
                        }
                    } else Spacer(Modifier.height(14.dp))
                }
            }
        }
        Row(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(24.dp)).background(cs.surfaceContainerHighest).padding(12.dp),
            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            ArtCover(track.art, Modifier.size(52.dp), RoundedCornerShape(14.dp), animated = playing)
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                AnimatedContent(target, transitionSpec = { (fadeIn() + slideInVertically { it }) togetherWith (fadeOut() + slideOutVertically { -it }) }, label = "on") { t ->
                    Text("${s.demoPlayingOn} ${devices[t].second}", style = MaterialTheme.typography.labelMedium, color = cs.primary, maxLines = 1)
                }
                Text("${track.title} · ${track.artist}", style = MaterialTheme.typography.titleSmall, color = cs.onSurface, maxLines = 1)
                LinearWavyProgressIndicator(
                    progress = { pos.value / 199f }, modifier = Modifier.fillMaxWidth(),
                    color = cs.primary, trackColor = cs.surfaceContainer, amplitude = { if (playing) 1f else 0f },
                )
            }
            FilledIconButton(onClick = { playing = !playing }, shapes = IconButtonDefaults.shapes()) {
                Icon(if (playing) Icons.Rounded.Pause else Icons.Rounded.PlayArrow, null)
            }
        }
    }
}

/** My Wave: the morphing button from the Android home screen, with vibe chips that retune the queue. */
@Composable
fun MyWaveDemo(modifier: Modifier = Modifier) {
    val s = site()
    val cs = MaterialTheme.colorScheme
    var vibe by remember { mutableIntStateOf(0) }
    var playing by remember { mutableStateOf(false) }
    val shapes = listOf(
        listOf(MaterialShapes.Cookie9Sided, MaterialShapes.Sunny, MaterialShapes.Flower),
        listOf(MaterialShapes.Puffy, MaterialShapes.Cookie12Sided, MaterialShapes.Ghostish),
        listOf(MaterialShapes.Clover4Leaf, MaterialShapes.Gem, MaterialShapes.Pill),
        listOf(MaterialShapes.SoftBurst, MaterialShapes.Burst, MaterialShapes.Cookie9Sided),
    )
    val tints = listOf(cs.primary, cs.tertiary, cs.secondary, cs.error)
    val blobColor by animateColorAsState(tints[vibe % tints.size], tween(500))
    val source = remember { MutableInteractionSource() }
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Box(Modifier.size(150.dp).springyPress(source, 0.9f, 1.04f).clickable(source, null) { playing = !playing }, contentAlignment = Alignment.Center) {
            key(vibe) {
                MorphingBlob(
                    Modifier.matchParentSize(), shapes = shapes[vibe % shapes.size], color = blobColor,
                    holdSeconds = if (playing) 0.9f else 2.4f, morphSeconds = 0.7f, spinDegPerSecond = if (playing) 30f else 6f,
                )
            }
            Icon(if (playing) Icons.Rounded.Pause else Icons.Rounded.PlayArrow, null, tint = cs.surface, modifier = Modifier.size(44.dp))
        }
        com.alananasss.kittytunewebsite.ui.kit.Wrap(hSpacing = 6.dp, vSpacing = 6.dp, center = true) {
            s.demoVibes.forEachIndexed { i, label ->
                ToggleButton(
                    checked = vibe == i, onCheckedChange = { vibe = i; playing = true },
                    contentPadding = PaddingValues(horizontal = 14.dp), modifier = Modifier.height(36.dp),
                ) { Text(label, style = MaterialTheme.typography.labelMedium) }
            }
        }
        AnimatedContent(vibe, transitionSpec = { (fadeIn() + slideInVertically { it / 3 }) togetherWith fadeOut() }, label = "queue") { v ->
            Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                (0 until 2).forEach { k ->
                    val tr = DemoTracks[(v * 2 + k + 1) % DemoTracks.size]
                    Row(
                        Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(cs.surfaceContainerHighest).padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        ArtCover(tr.art, Modifier.size(34.dp), RoundedCornerShape(10.dp))
                        Column(Modifier.weight(1f)) {
                            Text(tr.title, style = MaterialTheme.typography.labelLarge, color = cs.onSurface, maxLines = 1)
                            Text(tr.artist, style = MaterialTheme.typography.labelSmall, color = cs.onSurfaceVariant, maxLines = 1)
                        }
                    }
                }
            }
        }
    }
}

/** Offline downloads: choose the format, tap to download, watch it land in the library. */
@Composable
fun OfflineDemo(modifier: Modifier = Modifier) {
    val s = site()
    val cs = MaterialTheme.colorScheme
    val formats = listOf("MP3", "FLAC", "OPUS", "M4A")
    var format by remember { mutableIntStateOf(1) }
    val tracks = remember { DemoTracks.drop(1).take(4) }
    val progress = remember { tracks.map { Animatable(0f) } }
    val scope = rememberCoroutineScope()
    fun download(i: Int) {
        if (progress[i].value > 0f) {
            scope.launch { progress[i].snapTo(0f) }
            return
        }
        scope.launch { progress[i].animateTo(1f, tween(1400 + i * 250)) }
    }
    Column(modifier, verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            SegmentedGroup(formats, format, { format = it; scope.launch { progress.forEach { it.snapTo(0f) } } }, Modifier.weight(1f), height = 40.dp)
            FilledTonalButton(onClick = { tracks.indices.forEach { if (progress[it].value == 0f) download(it) } }, shapes = ButtonDefaults.shapes(), modifier = Modifier.height(40.dp)) {
                Icon(Icons.Rounded.DownloadForOffline, null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text(s.demoDownloadAll, style = MaterialTheme.typography.labelLarge, maxLines = 1)
            }
        }
        tracks.forEachIndexed { i, tr ->
            val p = progress[i].value
            Row(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(cs.surfaceContainerHighest).clickable { download(i) }.padding(10.dp),
                verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                ArtCover(tr.art, Modifier.size(40.dp), RoundedCornerShape(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(tr.title, style = MaterialTheme.typography.titleSmall, color = cs.onSurface, maxLines = 1)
                    Text(
                        if (p >= 1f) "${s.demoDownloaded} · ${formats[format]}" else "${tr.artist} · ${formatTime(tr.durationSec)}",
                        style = MaterialTheme.typography.labelMedium, color = if (p >= 1f) cs.primary else cs.onSurfaceVariant, maxLines = 1
                    )
                }
                Box(Modifier.size(40.dp), contentAlignment = Alignment.Center) {
                    when {
                        p <= 0f -> Icon(Icons.Rounded.Download, null, tint = cs.onSurfaceVariant)
                        p < 1f -> CircularWavyProgressIndicator(progress = { p }, modifier = Modifier.size(34.dp), color = cs.primary, trackColor = cs.surfaceContainer)
                        else -> Box(Modifier.size(34.dp).clip(CircleShape).background(cs.primary), contentAlignment = Alignment.Center) {
                            Icon(Icons.Rounded.Check, null, tint = cs.onPrimary, modifier = Modifier.size(20.dp))
                        }
                    }
                }
            }
        }
    }
}

private data class BlockedPlace(val lat: Float, val lon: Float)

/** Rough centres of the countries shown on the globe (degrees). */
private val blockedPlaces = listOf(
    BlockedPlace(61f, 95f),   // Russia
    BlockedPlace(35f, 103f),  // China
    BlockedPlace(32f, 53f),   // Iran
    BlockedPlace(39f, 59f),   // Turkmenistan
)

/** Evenly spread points on a sphere (Fibonacci lattice), as (lat, lon) in radians. */
private val globeDots: List<Pair<Float, Float>> by lazy {
    val n = 900
    val golden = (kotlin.math.PI * (3.0 - kotlin.math.sqrt(5.0))).toFloat()
    List(n) { i ->
        val y = 1f - (i + 0.5f) / n * 2f
        val lat = kotlin.math.asin(y)
        val lon = (golden * i) % (2f * kotlin.math.PI.toFloat())
        lat to lon
    }
}

/**
 * A slowly turning dotted globe. Pins pulse red where SoundCloud is blocked and turn to the theme
 * colour as [unlock] goes to 1. Everything is computed in the draw phase.
 */
@Composable
fun BlockGlobe(unlock: () -> Float, modifier: Modifier = Modifier) {
    val cs = MaterialTheme.colorScheme
    val clock = rememberClock()
    val dotColor = cs.onSurfaceVariant
    val sphere = cs.surfaceContainerHigh
    val rim = cs.outlineVariant
    val blocked = cs.error
    val open = cs.primary
    androidx.compose.foundation.Canvas(modifier) {
        val t = clock.value
        val r = size.minDimension / 2f * 0.9f
        val c = center
        val deg = kotlin.math.PI.toFloat() / 180f
        val lon0 = (70f + t * 9f) * deg
        val tilt = 22f * deg
        val ct = kotlin.math.cos(tilt)
        val st = kotlin.math.sin(tilt)
        fun project(lat: Float, lon: Float): Triple<Float, Float, Float> {
            val l = lon - lon0
            val cl = kotlin.math.cos(lat)
            val x = cl * kotlin.math.sin(l)
            val y = ct * kotlin.math.sin(lat) - st * cl * kotlin.math.cos(l)
            val z = st * kotlin.math.sin(lat) + ct * cl * kotlin.math.cos(l)
            return Triple(c.x + x * r, c.y - y * r, z)
        }
        drawCircle(sphere, r, c)
        drawCircle(rim, r, c, style = androidx.compose.ui.graphics.drawscope.Stroke(1.5f))
        val dotR = r * 0.012f
        for ((lat, lon) in globeDots) {
            val (x, y, z) = project(lat, lon)
            if (z <= 0f) continue
            drawCircle(dotColor.copy(alpha = 0.12f + 0.5f * z), dotR * (0.6f + 0.6f * z), androidx.compose.ui.geometry.Offset(x, y))
        }
        val u = unlock()
        val pinColor = androidx.compose.ui.graphics.lerp(blocked, open, u)
        blockedPlaces.forEachIndexed { i, p ->
            val (x, y, z) = project(p.lat * deg, p.lon * deg)
            if (z <= 0.05f) return@forEachIndexed
            val o = androidx.compose.ui.geometry.Offset(x, y)
            val phase = ((t * 0.7f + i * 0.27f) % 1f)
            // expanding ring
            drawCircle(pinColor.copy(alpha = (1f - phase) * 0.55f * z), r * (0.04f + 0.14f * phase), o, style = androidx.compose.ui.graphics.drawscope.Stroke(2.5f))
            drawCircle(pinColor.copy(alpha = 0.25f * z), r * 0.06f, o)
            drawCircle(pinColor.copy(alpha = z), r * 0.028f, o)
        }
    }
}

/**
 * The censorship showcase: pick a network mode and watch the blocked countries come back one by one,
 * on the globe and in the list.
 */
@Composable
fun UnblockShowcase(modifier: Modifier = Modifier) {
    val s = site()
    val cs = MaterialTheme.colorScheme
    var mode by remember { mutableIntStateOf(0) }
    var connecting by remember { mutableStateOf(false) }
    LaunchedEffect(mode) {
        if (mode == 0) { connecting = false; return@LaunchedEffect }
        connecting = true
        kotlinx.coroutines.delay(1100)
        connecting = false
    }
    val ok = mode != 0 && !connecting
    val unlock by animateFloatAsState(if (ok) 1f else 0f, tween(1100))
    BoxWithConstraints(modifier) {
        val wide = maxWidth >= 860.dp
        val globe: @Composable (Modifier) -> Unit = { m -> BlockGlobe({ unlock }, m.aspectRatio(1f)) }
        val panel: @Composable (Modifier) -> Unit = { m ->
            Column(m, verticalArrangement = Arrangement.spacedBy(14.dp)) {
                SegmentedGroup(
                    s.bypassModes, mode, { mode = it }, Modifier.fillMaxWidth(),
                    icons = listOf(Icons.Rounded.Block, Icons.Rounded.Shield, Icons.Rounded.VpnLock),
                )
                val places = s.blockCountries + s.blockNetworks
                Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    places.forEachIndexed { i, name ->
                        var on by remember { mutableStateOf(false) }
                        LaunchedEffect(ok, connecting) {
                            if (ok) { kotlinx.coroutines.delay(150L * i); on = true } else on = false
                        }
                        val big = 24.dp
                        val small = 6.dp
                        val shape = when (i) {
                            0 -> RoundedCornerShape(topStart = big, topEnd = big, bottomStart = small, bottomEnd = small)
                            places.lastIndex -> RoundedCornerShape(topStart = small, topEnd = small, bottomStart = big, bottomEnd = big)
                            else -> RoundedCornerShape(small)
                        }
                        val chipBg by animateColorAsState(if (on) cs.primaryContainer else if (connecting) cs.secondaryContainer else cs.errorContainer, tween(400))
                        val chipFg by animateColorAsState(if (on) cs.onPrimaryContainer else if (connecting) cs.onSecondaryContainer else cs.onErrorContainer, tween(400))
                        val pop by animateFloatAsState(if (on) 1f else 0f, spring(0.45f, 300f))
                        Row(
                            Modifier.fillMaxWidth().clip(shape).background(cs.surfaceContainer).padding(horizontal = 16.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Icon(
                                if (i == places.lastIndex) Icons.Rounded.School else Icons.Rounded.Public, null,
                                tint = cs.onSurfaceVariant, modifier = Modifier.size(20.dp)
                            )
                            Text(name, style = MaterialTheme.typography.titleSmall, color = cs.onSurface, modifier = Modifier.weight(1f), maxLines = 1)
                            Row(
                                Modifier.graphicsLayer { val sc = 1f + 0.08f * pop * (1f - pop) * 4f; scaleX = sc; scaleY = sc }
                                    .clip(CircleShape).background(chipBg).padding(horizontal = 12.dp, vertical = 5.dp),
                                verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(if (on) Icons.Rounded.LockOpen else Icons.Rounded.Lock, null, tint = chipFg, modifier = Modifier.size(14.dp))
                                Text(
                                    when {
                                        on -> s.blockStatusUnlocked
                                        i == places.lastIndex -> s.blockStatusFiltered
                                        else -> s.blockStatusBlocked
                                    },
                                    style = MaterialTheme.typography.labelMedium, color = chipFg, maxLines = 1
                                )
                            }
                        }
                    }
                }
                val statusBg by animateColorAsState(if (ok) cs.primaryContainer else if (connecting) cs.secondaryContainer else cs.surfaceContainerHighest)
                val statusFg by animateColorAsState(if (ok) cs.onPrimaryContainer else if (connecting) cs.onSecondaryContainer else cs.onSurfaceVariant)
                val track = DemoTracks[0]
                val pos = rememberPlayhead(ok, start = 42f, duration = track.durationSec.toFloat())
                Row(
                    Modifier.fillMaxWidth().clip(RoundedCornerShape(24.dp)).background(statusBg).padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(Modifier.size(48.dp), contentAlignment = Alignment.Center) {
                        if (connecting) LoadingIndicator(Modifier.size(48.dp), color = statusFg)
                        else ArtCover(track.art, Modifier.size(48.dp), RoundedCornerShape(14.dp), animated = ok)
                    }
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        AnimatedContent(Pair(ok, connecting), transitionSpec = { (fadeIn() + slideInVertically { it / 2 }) togetherWith fadeOut() }, label = "net") { (o, c) ->
                            Text(
                                when { c -> s.bypassConnecting; o -> "${track.title} · ${track.artist}"; else -> s.bypassBlocked },
                                style = MaterialTheme.typography.titleSmall, color = statusFg, maxLines = 1
                            )
                        }
                        LinearWavyProgressIndicator(
                            progress = { pos.value / track.durationSec }, modifier = Modifier.fillMaxWidth(),
                            color = statusFg, trackColor = statusFg.copy(alpha = 0.2f), amplitude = { if (ok) 1f else 0f },
                        )
                    }
                }
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Rounded.TouchApp, null, tint = cs.primary, modifier = Modifier.size(18.dp))
                    Text(s.blockHint, style = MaterialTheme.typography.labelLarge, color = cs.onSurfaceVariant)
                }
            }
        }
        if (wide) {
            Row(horizontalArrangement = Arrangement.spacedBy(32.dp), verticalAlignment = Alignment.CenterVertically) {
                globe(Modifier.weight(1f))
                panel(Modifier.weight(1f))
            }
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                globe(Modifier.fillMaxWidth(0.85f))
                panel(Modifier.fillMaxWidth())
            }
        }
    }
}
