@file:OptIn(ExperimentalMaterial3ExpressiveApi::class)

package com.alananasss.kittytunewebsite.ui.demos

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.alananasss.kittytunewebsite.ui.kit.springyPress
import com.alananasss.kittytunewebsite.ui.kit.Wrap
import com.alananasss.kittytunewebsite.ui.kit.lerp
import com.alananasss.kittytunewebsite.ui.kit.rememberClock
import com.alananasss.kittytunewebsite.ui.site
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.pow
import kotlin.math.sin

private data class Fx(val name: String, val icon: ImageVector)

private val fxList = listOf(
    Fx("Bass Boost", Icons.Rounded.Bolt),
    Fx("8D Audio", Icons.Rounded.SurroundSound),
    Fx("Muffled", Icons.Rounded.BlurOn),
    Fx("Reverb", Icons.Rounded.GraphicEq),
    Fx("Rain Mode", Icons.Rounded.WaterDrop),
)

/** The app's Audio Settings sheet, rebuilt: speed slider with stops, Pitch toggle and the FX tiles. */
@Composable
fun AudioFxDemo(modifier: Modifier = Modifier, compact: Boolean = false) {
    val s = site()
    val cs = MaterialTheme.colorScheme
    var speed by remember { mutableFloatStateOf(1.1f) }
    var pitch by remember { mutableStateOf(true) }
    var enabled by remember { mutableStateOf(setOf("8D Audio")) }
    Column(modifier, verticalArrangement = Arrangement.spacedBy(if (compact) 10.dp else 14.dp)) {
        if (!compact) Text(s.demoAudioSettings, style = MaterialTheme.typography.headlineSmall, color = cs.onSurface)
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Rounded.Speed, null, tint = cs.primary, modifier = Modifier.size(22.dp))
            Spacer(Modifier.width(12.dp))
            val shown = (kotlin.math.round(speed * 10f) / 10f).toString().removeSuffix(".0").let { if ('.' !in it) "$it.0" else it }
            Text("${shown}x", style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.W800), color = cs.onSurface, modifier = Modifier.weight(1f))
            val pitchBg by animateColorAsState(if (pitch) cs.primary else cs.surfaceContainerHighest)
            val pitchFg by animateColorAsState(if (pitch) cs.onPrimary else cs.onSurfaceVariant)
            Row(
                Modifier.height(40.dp).clip(CircleShape).background(pitchBg).clickable { pitch = !pitch }.padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                androidx.compose.animation.AnimatedVisibility(pitch) { Icon(Icons.Rounded.Check, null, tint = pitchFg, modifier = Modifier.size(18.dp)) }
                Text(s.demoPitch, style = MaterialTheme.typography.labelLarge, color = pitchFg)
            }
        }
        Slider(
            value = speed,
            onValueChange = { speed = it },
            valueRange = 0.5f..2.0f,
            steps = 14,
            modifier = Modifier.fillMaxWidth(),
        )
        Text(s.demoAudioFx, style = MaterialTheme.typography.titleSmall, color = cs.onSurfaceVariant, modifier = Modifier.padding(top = 4.dp))
        fxList.chunked(2).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                row.forEach { fx -> FxTile(fx, fx.name in enabled, compact, Modifier.weight(1f)) { enabled = if (fx.name in enabled) enabled - fx.name else enabled + fx.name } }
            }
        }
    }
}

@Composable
private fun FxTile(fx: Fx, on: Boolean, compact: Boolean, modifier: Modifier, onClick: () -> Unit) {
    val cs = MaterialTheme.colorScheme
    val bg by animateColorAsState(if (on) cs.primary else cs.surfaceContainerHigh)
    val fg by animateColorAsState(if (on) cs.onPrimary else cs.onSurface)
    val corner by androidx.compose.animation.core.animateDpAsState(if (on) 20.dp else 40.dp, spring(0.5f, 400f))
    val source = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
    Column(
        modifier
            .height(if (compact) 72.dp else 84.dp)
            .springyPress(source, 0.94f, 1.02f)
            .clip(RoundedCornerShape(corner))
            .background(bg)
            .clickable(source, null, onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(fx.icon, null, tint = fg, modifier = Modifier.size(if (compact) 22.dp else 26.dp))
        Spacer(Modifier.height(6.dp))
        Text(fx.name, style = MaterialTheme.typography.labelLarge, color = fg, maxLines = 1)
    }
}

private val eqProfiles = listOf("Over-ear", "In-ear", "Earbuds", "Studio")

/** Four stems with their own waveform; tap one to mute it. */
@Composable
fun StemsDemo(modifier: Modifier = Modifier) {
    val s = site()
    val cs = MaterialTheme.colorScheme
    val colors = listOf(cs.primary, cs.tertiary, cs.secondary, cs.error)
    var muted by remember { mutableStateOf(setOf<Int>()) }
    val clock = rememberClock()
    val icons = listOf(Icons.Rounded.Mic, Icons.Rounded.Album, Icons.Rounded.GraphicEq, Icons.Rounded.Piano)
    Column(modifier, verticalArrangement = Arrangement.spacedBy(10.dp)) {
        s.demoStems.forEachIndexed { i, name ->
            val on = i !in muted
            val alpha by animateFloatAsState(if (on) 1f else 0.25f)
            val bg by animateColorAsState(if (on) cs.surfaceContainerHighest else cs.surfaceContainerHigh)
            Row(
                Modifier.fillMaxWidth().height(46.dp).clip(RoundedCornerShape(23.dp)).background(bg)
                    .clickable { muted = if (on) muted + i else muted - i }.padding(horizontal = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(Modifier.size(30.dp).clip(CircleShape).background(colors[i].copy(alpha = alpha)), contentAlignment = Alignment.Center) {
                    Icon(if (on) icons[i] else Icons.Rounded.VolumeOff, null, tint = cs.surface, modifier = Modifier.size(16.dp))
                }
                Text(name, style = MaterialTheme.typography.labelLarge, modifier = Modifier.width(72.dp), color = cs.onSurface.copy(alpha = 0.4f + 0.6f * alpha), maxLines = 1)
                Canvas(Modifier.weight(1f).fillMaxHeight().padding(vertical = 10.dp)) {
                    val t = clock.value
                    val n = 48
                    val bw = size.width / n
                    val head = ((t * 0.12f) % 1f) * size.width
                    for (k in 0 until n) {
                        val seed = sin(k * 12.9898f + i * 78.233f) * 43758.5453f
                        val rnd = seed - kotlin.math.floor(seed)
                        val env = when (i) {
                            0 -> 0.3f + 0.7f * abs(sin(k * 0.35f))
                            1 -> if (k % 4 == 0) 1f else 0.25f + 0.2f * rnd
                            2 -> 0.5f + 0.3f * sin(k * 0.2f)
                            else -> 0.2f + 0.6f * rnd
                        }
                        val h = size.height * env * (0.6f + 0.4f * rnd)
                        val x = k * bw
                        val played = x < head
                        drawRoundRect(
                            colors[i].copy(alpha = alpha * if (played) 1f else 0.35f),
                            Offset(x + bw * 0.2f, (size.height - h) / 2f), Size(bw * 0.6f, h), CornerRadius(bw)
                        )
                    }
                }
            }
        }
    }
}

