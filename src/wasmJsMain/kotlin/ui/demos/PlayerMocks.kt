@file:OptIn(ExperimentalMaterial3ExpressiveApi::class)

package com.alananasss.kittytunewebsite.ui.demos

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.QueueMusic
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.alananasss.kittytunewebsite.ui.kit.LocalDemoActive
import com.alananasss.kittytunewebsite.ui.kit.rememberClock
import com.alananasss.kittytunewebsite.ui.kit.springyPress
import com.alananasss.kittytunewebsite.ui.site
import com.alananasss.kittytunewebsite.ui.lyrics.KaraokeLyricsView
import com.alananasss.kittytunewebsite.ui.lyrics.NeonTidesLyrics
import com.alananasss.kittytunewebsite.ui.lyrics.NeonTidesLoopMs
import kotlin.math.abs
import kittytunewebsite.generated.resources.Res
import kittytunewebsite.generated.resources.cover_chatiment

/** Playback position that only advances while [playing] and on screen. Writable, so demos can seek. */
@Composable
fun rememberPlayhead(playing: Boolean, start: Float = 47f, duration: Float = 204f): MutableFloatState {
    val active = LocalDemoActive.current
    val pos = remember { mutableFloatStateOf(start) }
    val dur by rememberUpdatedState(duration)
    LaunchedEffect(playing, active) {
        if (!playing || !active) return@LaunchedEffect
        var last = withFrameNanos { it }
        while (true) {
            withFrameNanos { now ->
                pos.floatValue = (pos.floatValue + (now - last) / 1_000_000_000f) % dur
                last = now
            }
        }
    }
    return pos
}

/** The song both lyrics demos play: a real track, with its sleeve and lyrics fetched live. */
private val LyricsSong = com.alananasss.kittytunewebsite.ui.lyrics.Chatiment

/** Lines to show: live LRCLIB lyrics when they have arrived, the original demo song until then. */
@Composable
private fun rememberDemoLines(): Pair<List<com.alananasss.kittytunewebsite.ui.lyrics.KLine>, Int> {
    val live by com.alananasss.kittytunewebsite.ui.lyrics.rememberLiveLyrics(LyricsSong)
    val l = live
    return if (l != null) l to LyricsSong.durationMs else NeonTidesLyrics to NeonTidesLoopMs
}

@Composable
private fun SleeveCover(modifier: Modifier, corner: Dp) {
    androidx.compose.foundation.Image(
        org.jetbrains.compose.resources.painterResource(Res.drawable.cover_chatiment),
        "${LyricsSong.title} · ${LyricsSong.artist}",
        contentScale = androidx.compose.ui.layout.ContentScale.Crop,
        modifier = modifier.clip(RoundedCornerShape(corner)),
    )
}

/** The desktop full-screen player: fluid sleeve background, real karaoke engine, working transport. */
@Composable
fun DesktopLyricsMock(modifier: Modifier = Modifier) {
    var playing by remember { mutableStateOf(true) }
    val (lines, loopMs) = rememberDemoLines()
    val loopSec = loopMs / 1000f
    val firstSec = (lines.firstOrNull()?.start ?: 0) / 1000f
    val pos = rememberPlayhead(playing, start = (firstSec - 1.5f).coerceAtLeast(0f), duration = loopSec)
    LaunchedEffect(loopMs) { pos.floatValue = (firstSec - 1.5f).coerceAtLeast(0f) }
    WindowFrame(modifier, title = "KittyTune · ${LyricsSong.title}") {
        Box(Modifier.fillMaxWidth().height(440.dp)) {
            FluidArtwork(Res.drawable.cover_chatiment, Modifier.matchParentSize())
            Row(Modifier.fillMaxSize().padding(horizontal = 36.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.width(250.dp)) {
                    val coverScale by animateFloatAsState(if (playing) 1f else 0.9f, spring(dampingRatio = 0.6f, stiffness = 300f))
                    SleeveCover(Modifier.fillMaxWidth().aspectRatio(1f).graphicsLayer { scaleX = coverScale; scaleY = coverScale }, 18.dp)
                    Spacer(Modifier.height(16.dp))
                    Text(LyricsSong.title, style = MaterialTheme.typography.titleMedium, color = Color.White, maxLines = 1)
                    Text(LyricsSong.artist, style = MaterialTheme.typography.bodySmall, color = Color.White.copy(alpha = 0.7f))
                    Spacer(Modifier.height(12.dp))
                    LinearWavyProgressIndicator(
                        progress = { pos.value / loopSec },
                        modifier = Modifier.fillMaxWidth(),
                        color = Color.White,
                        trackColor = Color.White.copy(alpha = 0.2f),
                        amplitude = { if (playing) 1f else 0f },
                    )
                    Spacer(Modifier.height(14.dp))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly, verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Rounded.SkipPrevious, null, tint = Color.White, modifier = Modifier.size(24.dp))
                        val source = remember { MutableInteractionSource() }
                        Box(
                            Modifier.size(52.dp).springyPress(source, 0.86f, 1.08f).clip(if (playing) RoundedCornerShape(16.dp) else CircleShape).background(Color.White).clickable(source, null) { playing = !playing },
                            contentAlignment = Alignment.Center
                        ) { Icon(if (playing) Icons.Rounded.Pause else Icons.Rounded.PlayArrow, null, tint = Color.Black, modifier = Modifier.size(26.dp)) }
                        Icon(Icons.Rounded.SkipNext, null, tint = Color.White, modifier = Modifier.size(24.dp))
                    }
                }
                Spacer(Modifier.width(48.dp))
                KaraokeLyricsView(
                    lines = lines,
                    timeMs = { (pos.value * 1000f).toInt() },
                    modifier = Modifier.weight(1f).fillMaxHeight(),
                    textStyle = MaterialTheme.typography.headlineLarge.copy(fontSize = 32.sp, lineHeight = 40.sp, fontWeight = FontWeight.Bold),
                    anchorFraction = 0.36f,
                )
            }
        }
    }
}

/**
 * Full-screen-lyrics card, like the desktop player: the sleeve's fluid background, cover and title,
 * and the real karaoke engine. Tap the cover to pause.
 */
@Composable
fun LyricsCardDemo(modifier: Modifier = Modifier) {
    var playing by remember { mutableStateOf(true) }
    val (lines, loopMs) = rememberDemoLines()
    val loopSec = loopMs / 1000f
    val firstSec = (lines.firstOrNull()?.start ?: 0) / 1000f
    val pos = rememberPlayhead(playing, start = (firstSec - 1f).coerceAtLeast(0f), duration = loopSec)
    LaunchedEffect(loopMs) { pos.floatValue = (firstSec - 1f).coerceAtLeast(0f) }
    BoxWithConstraints(modifier.clip(RoundedCornerShape(28.dp))) {
        val wide = maxWidth >= 560.dp
        FluidArtwork(Res.drawable.cover_chatiment, Modifier.matchParentSize())
        val lyrics: @Composable (Modifier) -> Unit = { m ->
            KaraokeLyricsView(
                lines = lines,
                timeMs = { (pos.value * 1000f).toInt() },
                modifier = m,
                textStyle = MaterialTheme.typography.headlineMedium.copy(fontSize = if (wide) 26.sp else 23.sp, lineHeight = if (wide) 33.sp else 29.sp, fontWeight = FontWeight.Bold),
                anchorFraction = 0.3f,
            )
        }
        val source = remember { MutableInteractionSource() }
        val coverScale by animateFloatAsState(if (playing) 1f else 0.88f, spring(dampingRatio = 0.55f, stiffness = 320f))
        if (wide) {
            Row(Modifier.fillMaxSize().padding(24.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(28.dp)) {
                Column(Modifier.width(184.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    SleeveCover(
                        Modifier.fillMaxWidth().aspectRatio(1f).graphicsLayer { scaleX = coverScale; scaleY = coverScale }
                            .springyPress(source, 0.94f, 1.02f).clickable(source, null) { playing = !playing },
                        16.dp
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(LyricsSong.title, style = MaterialTheme.typography.titleMedium, color = Color.White, maxLines = 1)
                    Text(LyricsSong.artist, style = MaterialTheme.typography.bodySmall, color = Color.White.copy(alpha = 0.7f), maxLines = 1)
                    Spacer(Modifier.height(4.dp))
                    LinearWavyProgressIndicator(
                        progress = { pos.value / loopSec },
                        modifier = Modifier.fillMaxWidth(),
                        color = Color.White,
                        trackColor = Color.White.copy(alpha = 0.22f),
                        amplitude = { if (playing) 1f else 0f },
                    )
                }
                lyrics(Modifier.weight(1f).fillMaxHeight())
            }
        } else {
            Column(Modifier.fillMaxSize().padding(horizontal = 20.dp, vertical = 16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    SleeveCover(Modifier.size(48.dp).graphicsLayer { scaleX = coverScale; scaleY = coverScale }.clickable(source, null) { playing = !playing }, 12.dp)
                    Column(Modifier.weight(1f)) {
                        Text(LyricsSong.title, style = MaterialTheme.typography.labelLarge, color = Color.White)
                        Text(LyricsSong.artist, style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = 0.7f))
                    }
                }
                lyrics(Modifier.fillMaxWidth().weight(1f))
            }
        }
    }
}
