package com.alananasss.kittytunewebsite.ui.kit

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalWindowInfo
import kotlin.math.PI
import kotlin.math.sin

/** Whether the demos below this point are on screen. Clocks freeze while false to keep the page light. */
val LocalDemoActive = compositionLocalOf { true }

/** Seconds elapsed while the surrounding [ViewportGate] is visible. Read it in draw/layer lambdas. */
@Composable
fun rememberClock(speed: Float = 1f): State<Float> {
    val active = LocalDemoActive.current
    val time = remember { mutableFloatStateOf(0f) }
    LaunchedEffect(active, speed) {
        if (!active) return@LaunchedEffect
        var last = withFrameNanos { it }
        while (true) {
            withFrameNanos { now ->
                time.floatValue += ((now - last) / 1_000_000_000f) * speed
                last = now
            }
        }
    }
    return time
}

/** Gates every [rememberClock] inside [content] on whether this box intersects the window. */
@Composable
fun ViewportGate(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    var visible by remember { mutableStateOf(false) }
    val windowHeight = LocalWindowInfo.current.containerSize.height.toFloat()
    Box(
        modifier.onGloballyPositioned { c ->
            val b = c.boundsInWindow()
            val h = if (windowHeight > 0f) windowHeight else 2000f
            visible = b.bottom > -80f && b.top < h + 80f && b.height > 0f
        }
    ) {
        CompositionLocalProvider(LocalDemoActive provides visible) { content() }
    }
}

/** Fades and lifts content in the first time it scrolls into view. */
fun Modifier.reveal(delayMs: Int = 0, distance: Float = 56f): Modifier = composed {
    var seen by remember { mutableStateOf(false) }
    val windowHeight = LocalWindowInfo.current.containerSize.height.toFloat()
    val progress = remember { Animatable(0f) }
    LaunchedEffect(seen) {
        if (seen) {
            kotlinx.coroutines.delay(delayMs.toLong())
            progress.animateTo(1f, spring(dampingRatio = 0.8f, stiffness = 140f))
        }
    }
    this
        .onGloballyPositioned { c ->
            if (!seen) {
                val h = if (windowHeight > 0f) windowHeight else 900f
                if (c.boundsInWindow().top < h * 0.94f) seen = true
            }
        }
        .graphicsLayer {
            val p = progress.value
            alpha = p.coerceIn(0f, 1f)
            translationY = (1f - p) * distance
            val s = 0.96f + 0.04f * p
            scaleX = s; scaleY = s
        }
}

/** Expressive squish: shrinks on press, grows a touch on hover. */
fun Modifier.springyPress(source: MutableInteractionSource, pressed: Float = 0.94f, hovered: Float = 1.02f): Modifier = composed {
    val isPressed by source.collectIsPressedAsState()
    val isHovered by source.collectIsHoveredAsState()
    val target = when {
        isPressed -> pressed
        isHovered -> hovered
        else -> 1f
    }
    val scale by animateFloatAsState(target, spring(dampingRatio = 0.45f, stiffness = Spring.StiffnessMediumLow))
    graphicsLayer { scaleX = scale; scaleY = scale }
}

fun wave(t: Float, period: Float, phase: Float = 0f): Float = sin(((t / period) + phase) * 2f * PI.toFloat())

fun easeInOut(x: Float): Float {
    val c = x.coerceIn(0f, 1f)
    return if (c < 0.5f) 4f * c * c * c else 1f - ((-2f * c + 2f).let { it * it * it }) / 2f
}

fun lerp(a: Float, b: Float, t: Float) = a + (b - a) * t

/**
 * Lays [content] out at a fixed design width, then scales it down (never up past [maxScale]) to the
 * available width. Mocks stay pixel-identical from phones to 4K screens.
 */
@Composable
fun ScaleToFit(
    designWidth: androidx.compose.ui.unit.Dp,
    modifier: Modifier = Modifier,
    maxScale: Float = 1f,
    content: @Composable () -> Unit,
) {
    androidx.compose.ui.layout.Layout(content, modifier) { measurables, constraints ->
        val dw = designWidth.roundToPx()
        val p = measurables.first().measure(androidx.compose.ui.unit.Constraints(minWidth = dw, maxWidth = dw))
        val avail = if (constraints.hasBoundedWidth) constraints.maxWidth else dw
        val scale = (avail.toFloat() / dw).coerceAtMost(maxScale)
        val w = (dw * scale).toInt()
        val h = (p.height * scale).toInt()
        layout(w, h) {
            p.placeWithLayer(0, 0) {
                scaleX = scale; scaleY = scale
                transformOrigin = androidx.compose.ui.graphics.TransformOrigin(0f, 0f)
            }
        }
    }
}

/**
 * Counts from 0 to [total], one step per frame. Pages use it to compose their sections over several
 * frames rather than all at once, which is what made opening a tab stutter.
 */
@Composable
fun rememberStage(total: Int): State<Int> {
    val stage = remember { androidx.compose.runtime.mutableIntStateOf(0) }
    LaunchedEffect(total) {
        while (stage.intValue < total) {
            withFrameNanos { }
            stage.intValue++
        }
    }
    return stage
}
