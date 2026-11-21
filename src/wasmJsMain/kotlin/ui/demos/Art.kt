@file:OptIn(ExperimentalMaterial3ExpressiveApi::class)

package com.alananasss.kittytunewebsite.ui.demos

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.rounded.Wifi
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.Image
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import kittytunewebsite.generated.resources.*
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource
import com.alananasss.kittytunewebsite.ui.kit.rememberClock
import com.alananasss.kittytunewebsite.ui.kit.wave
import kotlin.math.cos
import kotlin.math.sin

/** The catalogue every mock plays from: real SoundCloud uploads, with their own sleeves. */
data class DemoTrack(val title: String, val artist: String, val cover: DrawableResource, val durationSec: Int) {
    /** Index-compatible handle for [ArtCover]. */
    val art: Int get() = DemoTracks.indexOf(this).coerceAtLeast(0)
}

val DemoTracks: List<DemoTrack> by lazy {
    listOf(
        DemoTrack("Всё Что Нас Не Убивает (slowed)", "Pimp Schwab", Res.drawable.cover_vse_slowed, 335),
        DemoTrack("lune + fairy", "glint", Res.drawable.cover_lune_fairy, 96),
        DemoTrack("consciousness", "mari, exhaustedkxlla", Res.drawable.cover_consciousness, 201),
        DemoTrack("eternal addict II", "mari, exhaustedkxlla", Res.drawable.cover_eternal_addict, 217),
        DemoTrack("the black hole is too close!", "Loli in early 20s", Res.drawable.cover_black_hole, 110),
        DemoTrack("Spaces In-Between", "OMORI OST", Res.drawable.cover_spaces_in_between, 59),
        DemoTrack("Люблю", "Nicebeatzprod.", Res.drawable.cover_lyublyu, 130),
        DemoTrack("Все что нас не убивает (bassboost)", "Pimp Schwab", Res.drawable.cover_vse_bass, 67),
    )
}

/**
 * A track's real sleeve. [animated] adds a slow drift, like the moving artwork the app plays; it is
 * applied in the layer, so it never recomposes.
 */
@Composable
fun ArtCover(art: Int, modifier: Modifier = Modifier, shape: Shape = RoundedCornerShape(20.dp), animated: Boolean = false) {
    val track = DemoTracks[art % DemoTracks.size]
    val clock = if (animated) rememberClock() else null
    Box(modifier.clip(shape)) {
        Image(
            painterResource(track.cover), "${track.title} · ${track.artist}",
            contentScale = ContentScale.Crop,
            modifier = Modifier.matchParentSize().graphicsLayer {
                val t = clock?.value ?: return@graphicsLayer
                val sc = 1.06f + 0.04f * wave(t, 9f)
                scaleX = sc; scaleY = sc
                translationX = size.width * 0.015f * wave(t, 11f, 0.3f)
            }
        )
    }
}

/** A desktop window with a title bar. */
@Composable
fun WindowFrame(modifier: Modifier = Modifier, title: String = "KittyTune", content: @Composable BoxScope.() -> Unit) {
    val cs = MaterialTheme.colorScheme
    Column(
        modifier
            .shadow(40.dp, RoundedCornerShape(22.dp), ambientColor = Color.Black, spotColor = Color.Black)
            .clip(RoundedCornerShape(22.dp))
            .background(cs.surfaceContainerLowest)
            .border(1.dp, cs.outlineVariant.copy(alpha = 0.5f), RoundedCornerShape(22.dp))
    ) {
        Row(
            Modifier.fillMaxWidth().background(cs.surfaceContainer).padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(7.dp)
        ) {
            listOf(Color(0xFFFF5F57), Color(0xFFFEBC2E), Color(0xFF28C840)).forEach {
                Box(Modifier.size(11.dp).clip(CircleShape).background(it))
            }
            Spacer(Modifier.width(8.dp))
            Text(title, style = MaterialTheme.typography.labelMedium, color = cs.onSurfaceVariant)
        }
        Box(Modifier.fillMaxWidth(), content = content)
    }
}

/** Circular watch case. */
@Composable
fun WatchFrame(modifier: Modifier = Modifier, size: Dp = 220.dp, content: @Composable BoxScope.() -> Unit) {
    val cs = MaterialTheme.colorScheme
    Box(modifier.size(size * 1.12f), contentAlignment = Alignment.Center) {
        Canvas(Modifier.matchParentSize()) {
            // strap stubs + crown
            drawRoundRect(Color(0xFF1C1C22), topLeft = Offset(this.size.width * 0.28f, 0f), size = Size(this.size.width * 0.44f, this.size.height), cornerRadius = androidx.compose.ui.geometry.CornerRadius(40f))
            drawRoundRect(Color(0xFF2A2A33), topLeft = Offset(this.size.width * 0.95f, this.size.height * 0.42f), size = Size(this.size.width * 0.06f, this.size.height * 0.16f), cornerRadius = androidx.compose.ui.geometry.CornerRadius(8f))
            drawCircle(Color(0xFF26262E), radius = this.size.minDimension * 0.47f)
            drawCircle(cs.primary.copy(alpha = 0.5f), radius = this.size.minDimension * 0.47f, style = Stroke(3f))
        }
        Box(Modifier.size(size * 0.9f).clip(CircleShape).background(Color.Black), contentAlignment = Alignment.Center, content = content)
    }
}

fun formatTime(sec: Int): String {
    val s = sec.coerceAtLeast(0)
    val m = s / 60
    val r = s % 60
    return "$m:${if (r < 10) "0" else ""}$r"
}
