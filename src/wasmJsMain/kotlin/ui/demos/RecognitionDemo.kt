@file:OptIn(ExperimentalMaterial3ExpressiveApi::class)

package com.alananasss.kittytunewebsite.ui.demos

/*
 * Web port of KittyTune Desktop's recognition screen (ui/recognition): the Pixel "Now Playing" listen
 * button (circle → scalloped morph, squash-and-bloom, breathing halo), the animated searching bars,
 * the app's own Lottie blob background and the AGSL glow — same timings, same curves.
 */

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.center
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.graphics.shapes.CornerRounding
import androidx.graphics.shapes.Morph
import androidx.graphics.shapes.RoundedPolygon
import androidx.graphics.shapes.circle
import androidx.graphics.shapes.star
import com.alananasss.kittytunewebsite.ui.kit.LocalDemoActive
import com.alananasss.kittytunewebsite.ui.kit.rememberClock
import com.alananasss.kittytunewebsite.ui.site
import io.github.alexzhirkevich.compottie.Compottie
import io.github.alexzhirkevich.compottie.LottieCompositionSpec
import io.github.alexzhirkevich.compottie.dynamic.rememberLottieDynamicProperties
import io.github.alexzhirkevich.compottie.rememberLottieComposition
import io.github.alexzhirkevich.compottie.rememberLottiePainter
import kittytunewebsite.generated.resources.Res
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.min

private val EmphasizedDecelerate: Easing = CubicBezierEasing(0.1f, 0.7f, 0.1f, 1f)
private val StandardAccelerate: Easing = CubicBezierEasing(0.3f, 0f, 1f, 1f)
private const val SQUASH_SCALE = 0.95f
private const val BLOOM_SCALE = 1.3111111f
private const val PULSE_PERIOD_S = 1.5f
private const val PULSE_MIN_SCALE = 0.70f
private const val PULSE_PEAK = 0.25f
private val PULSE_SCALE_EASING = CubicBezierEasing(0.0f, 0.0f, 0.2f, 1.0f)
private val PULSE_RISE_EASING = CubicBezierEasing(0.4f, 0.0f, 1.0f, 1.0f)

private val ButtonMorph by lazy {
    Morph(
        RoundedPolygon.circle(numVertices = 10, radius = 1f),
        RoundedPolygon.star(numVerticesPerRadius = 10, radius = 1f, innerRadius = 0.8f, rounding = CornerRounding(0.33333334f))
    )
}

private fun Morph.toPath(progress: Float, path: Path): Path {
    path.reset()
    val cubics = asCubics(progress)
    if (cubics.isEmpty()) return path
    path.moveTo(cubics.first().anchor0X, cubics.first().anchor0Y)
    cubics.forEach { c -> path.cubicTo(c.control0X, c.control0Y, c.control1X, c.control1Y, c.anchor1X, c.anchor1Y) }
    path.close()
    return path
}

/** The Now Playing glyph, traced from the Pixel app's resources (960-unit viewport). */
private val NowPlayingNote: ImageVector by lazy {
    ImageVector.Builder("NowPlayingNote", 24.dp, 24.dp, 960f, 960f).addPath(
        pathData = PathParser().parsePathString(
            "M581.33,661.71q-16.67-16.63-16.67-40.38V258q0-23.75 16.61-40.38T621.61,201t40.23," +
                "16.63T678.33,258V621.33q0,23.75-16.54,40.38t-40.17,16.63t-40.29-16.63ZM823.29,557q-23.63," +
                "0-40.29-16.53t-16.67-40.14V379q0-23.61 16.61-40.14t40.33-16.53t40.23,16.53T880,379V500.33" +
                "q0,23.61-16.54,40.14T823.29,557ZM258.33,880q-91.16,0-154.58-63.52T40.33,661.65t63.42-154." +
                "81t154.58-63.5q28.67,0 54.83,7.17T363,470.33V136.67q0-23.61 16.54-40.14T419.71,80T460,96." +
                "53t16.67,40.14v525q0,91.3-63.52,154.82T258.33,880Z"
        ).toNodes(),
        fill = SolidColor(Color.Black),
    ).build()
}

/** circle → scalloped listen button with squash & bloom and the breathing halo. */
@Composable
private fun NowPlayingListenButton(
    active: Boolean,
    color: Color,
    haloColor: Color,
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit,
) {
    val buttonScale = remember { Animatable(1f) }
    val morph = remember { Animatable(0f) }
    val breathing = remember { Animatable(0f) }
    var first by remember { mutableStateOf(true) }
    LaunchedEffect(active) {
        if (first) { first = false; return@LaunchedEffect }
        if (!active) {
            launch { breathing.animateTo(0f, tween(250, easing = FastOutSlowInEasing)) }
            launch { morph.animateTo(0f, tween(350, easing = FastOutSlowInEasing)) }
            buttonScale.animateTo(SQUASH_SCALE, tween(150, easing = EmphasizedDecelerate))
            delay(50)
            buttonScale.animateTo(1f, tween(250, easing = StandardAccelerate))
        } else {
            launch { morph.animateTo(1f, tween(400, easing = FastOutSlowInEasing)) }
            buttonScale.animateTo(SQUASH_SCALE, tween(200, easing = EmphasizedDecelerate))
            delay(83)
            buttonScale.animateTo(BLOOM_SCALE, tween(300, easing = StandardAccelerate))
        }
    }
    LaunchedEffect(active) {
        if (!active) return@LaunchedEffect
        delay(1000)
        breathing.animateTo(8f, tween(750, easing = CubicBezierEasing(0f, 0f, 0.2f, 1f)))
        while (true) {
            breathing.animateTo(-8f, tween(1500, easing = FastOutSlowInEasing))
            breathing.animateTo(8f, tween(1500, easing = FastOutSlowInEasing))
        }
    }
    val clock = rememberClock()
    val haloFade by animateFloatAsState(if (active) 1f else 0f, tween(300))
    val haloPath = remember { Path().also { ButtonMorph.toPath(1f, it) } }
    val scratch = remember { Path() }
    Box(
        modifier.drawBehind {
            val phase = (clock.value % PULSE_PERIOD_S) / PULSE_PERIOD_S
            val a = if (phase < 0.5f) PULSE_RISE_EASING.transform(phase * 2f) * PULSE_PEAK
            else (1f - LinearEasing.transform((phase - 0.5f) * 2f)) * PULSE_PEAK
            val haloAlpha = a * haloFade
            if (haloAlpha <= 0f) return@drawBehind
            val r = (size.minDimension * (314f / 180f) / 2f) * (PULSE_MIN_SCALE + (1f - PULSE_MIN_SCALE) * PULSE_SCALE_EASING.transform(phase))
            translate(size.center.x, size.center.y) {
                scale(r, r, pivot = Offset.Zero) { rotate(-90f, pivot = Offset.Zero) { drawPath(haloPath, haloColor, alpha = haloAlpha, style = Fill) } }
            }
        },
        contentAlignment = Alignment.Center,
    ) {
        Box(
            Modifier
                .matchParentSize()
                .graphicsLayer { scaleX = buttonScale.value; scaleY = buttonScale.value }
                .drawBehind {
                    val radius = min(size.width, size.height) / 2f + breathing.value.dp.toPx() / 2f
                    ButtonMorph.toPath(morph.value, scratch)
                    translate(size.center.x, size.center.y) {
                        scale(radius, radius, pivot = Offset.Zero) { rotate(-90f, pivot = Offset.Zero) { drawPath(scratch, color, style = Fill) } }
                    }
                },
            contentAlignment = Alignment.Center,
            content = content,
        )
    }
}

private val BarsEase = CubicBezierEasing(0.5f, 0f, 0.5f, 1f)

/** `avd_nowplaying_searching`: note head + three bars bouncing out of phase. */
@Composable
private fun SearchingBarsIcon(color: Color, modifier: Modifier) {
    val d1 = remember { Animatable(0f) }
    val d2 = remember { Animatable(0f) }
    val d3 = remember { Animatable(0f) }
    val amp = 1f / 24f
    LaunchedEffect(Unit) {
        launch { d1.animateTo(-amp, tween(367, easing = BarsEase)); while (true) { d1.animateTo(amp, tween(375, easing = BarsEase)); d1.animateTo(-amp, tween(375, easing = BarsEase)) } }
        launch { d2.animateTo(amp, tween(500, easing = BarsEase)); while (true) { d2.animateTo(-amp, tween(500, easing = BarsEase)); d2.animateTo(amp, tween(500, easing = BarsEase)) } }
        launch { d3.animateTo(-amp, tween(750, easing = BarsEase)); while (true) { d3.animateTo(amp, tween(750, easing = BarsEase)); d3.animateTo(-amp, tween(750, easing = BarsEase)) } }
    }
    Canvas(modifier) {
        val w = size.width
        val h = size.height
        val bw = w * 3f / 24f
        val cr = CornerRadius(bw / 2f, bw / 2f)
        fun bar(cx: Float, cy: Float, hh: Float, dy: Float) =
            drawRoundRect(color, Offset(w * cx - bw / 2f, h * cy + dy * h - h * hh / 2f), Size(bw, h * hh), cr)
        drawCircle(color, w * 5.5f / 24f, Offset(w * 6.5f / 24f, h * 17.436f / 24f + d1.value * h))
        bar(10.5f / 24f, 10.637f / 24f, 12.375f / 24f, d1.value)
        bar(15.5f / 24f, 9.5f / 24f, 9f / 24f, d2.value)
        bar(20.5f / 24f, 13f / 24f, 3f / 24f, d3.value)
    }
}

/** The app's `background_animation.json`, re-tinted to the theme, blooming 1→5× when listening. */
@Composable
private fun BlobBackground(active: Boolean, modifier: Modifier) {
    val cs = MaterialTheme.colorScheme
    val json by produceState<String?>(null) { value = runCatching { Res.readBytes("files/lottie/background_animation.json").decodeToString() }.getOrNull() }
    val j = json ?: return
    val composition by rememberLottieComposition { LottieCompositionSpec.JsonString(j) }
    val scale = remember { Animatable(1f) }
    LaunchedEffect(active) { if (active) scale.animateTo(5f, tween(1000, easing = CubicBezierEasing(0.05f, 0.7f, 0.1f, 1f))) else scale.snapTo(1f) }
    val primary = cs.primary
    val secondary = cs.secondary
    val tertiary = cs.tertiary
    val dyn = rememberLottieDynamicProperties(primary, secondary, tertiary) {
        shapeLayer(".primary") { fill("Fill 1") { color { _ -> primary }; opacity { _ -> 0.95f } } }
        shapeLayer(".secondary") { fill("Fill 1") { color { _ -> secondary }; opacity { _ -> 0.95f } } }
        shapeLayer(".tertiary") { fill("Fill 1") { color { _ -> tertiary }; opacity { _ -> 0.90f } } }
    }
    val painter = rememberLottiePainter(
        composition = composition,
        dynamicProperties = dyn,
        iterations = Compottie.IterateForever,
        isPlaying = LocalDemoActive.current,
    )
    Image(painter, null, contentScale = ContentScale.Crop, modifier = modifier.graphicsLayer { scaleX = scale.value; scaleY = scale.value })
}

/** The AGSL simplex-noise glow at the bottom of the screen (SkSL runs it unchanged). */
@Composable
private fun GlowView(color: Color, modifier: Modifier) {
    val src by produceState<String?>(null) { value = runCatching { Res.readBytes("files/lottie/glow.agsl").decodeToString() }.getOrNull() }
    val effect = remember(src) { src?.let { runCatching { org.jetbrains.skia.RuntimeEffect.makeForShader(it) }.getOrNull() } }
    val clock = rememberClock()
    val builder = remember(effect) { effect?.let { org.jetbrains.skia.RuntimeShaderBuilder(it) } }
    Box(
        modifier.drawBehind {
            val b = builder
            if (b == null) {
                drawRect(Brush.verticalGradient(listOf(Color.Transparent, color.copy(alpha = 0.35f))))
                return@drawBehind
            }
            b.uniform("iResolution", size.width, size.height)
            b.uniform("iTime", clock.value)
            b.uniform("iColor", color.red, color.green, color.blue, color.alpha)
            val shader = runCatching { b.makeShader(null) }.getOrNull() ?: return@drawBehind
            val paint = org.jetbrains.skia.Paint().apply { this.shader = shader }
            drawIntoCanvas { c ->
                c.nativeCanvas.drawRect(org.jetbrains.skia.Rect.makeWH(size.width, size.height), paint)
            }
        }
    )
}

/** Recognition screen: tap the button, it listens, then shows a result — like the app. */
@Composable
fun ShazamDemo(modifier: Modifier = Modifier, height: Dp = 400.dp) {
    val s = site()
    val cs = MaterialTheme.colorScheme
    var state by remember { mutableIntStateOf(0) } // 0 idle, 1 searching, 2 success
    var resultIdx by remember { mutableIntStateOf(2) }
    LaunchedEffect(state) {
        if (state == 1) { delay(4200); resultIdx = (resultIdx + 3) % DemoTracks.size; state = 2 }
        if (state == 2) { delay(6000); state = 0 }
    }
    val searching = state == 1
    val bg by animateColorAsState(
        when (state) { 2 -> cs.surfaceContainerHigh; 1 -> cs.primaryContainer; else -> cs.secondaryContainer },
        tween(1000)
    )
    Box(modifier.height(height).clip(RoundedCornerShape(28.dp)).background(bg)) {
        androidx.compose.animation.AnimatedVisibility(state != 2, enter = fadeIn(tween(800)), exit = fadeOut(tween(500)), modifier = Modifier.matchParentSize()) {
            BlobBackground(searching, Modifier.fillMaxSize())
        }
        androidx.compose.animation.AnimatedVisibility(state != 2, enter = fadeIn(tween(800)), exit = fadeOut(tween(500)), modifier = Modifier.align(Alignment.BottomCenter)) {
            GlowView(cs.primary, Modifier.fillMaxWidth().height(120.dp))
        }
        AnimatedContent(
            state == 2,
            transitionSpec = {
                (fadeIn(tween(400)) + slideInVertically(tween(400)) { it / 16 }) togetherWith (fadeOut(tween(300)) + slideOutVertically(tween(300)) { -it / 16 })
            },
            label = "rec",
            modifier = Modifier.fillMaxSize()
        ) { success ->
            if (!success) {
                val source = remember { MutableInteractionSource() }
                val pressed by source.collectIsPressedAsState()
                val btnScale by animateFloatAsState(if (pressed && !searching) 0.92f else 1f)
                val labelColor by animateColorAsState(if (searching) cs.onPrimaryContainer else cs.onSecondaryContainer, tween(300))
                val labelOffset by animateDpAsState(if (searching) 21.dp else (-31).dp, tween(1000, easing = CubicBezierEasing(0.2f, 0f, 0f, 1f)))
                Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally) {
                    Spacer(Modifier.weight(1f))
                    NowPlayingListenButton(
                        active = searching,
                        color = if (searching) cs.onPrimaryContainer else cs.secondary,
                        haloColor = cs.onPrimaryContainer,
                        modifier = Modifier.size(height * 0.3f).scale(btnScale).clickable(source, null) { state = if (searching) 0 else 1 }
                    ) {
                        if (searching) SearchingBarsIcon(cs.primaryContainer, Modifier.size(height * 0.093f))
                        else Icon(NowPlayingNote, null, tint = cs.onSecondary, modifier = Modifier.size(height * 0.093f))
                    }
                    Spacer(Modifier.height(height * 0.16f))
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.offset { IntOffset(0, labelOffset.roundToPx()) }) {
                        AnimatedContent(if (searching) s.demoListening else s.demoTapToIdentify, transitionSpec = { fadeIn(tween(300)) togetherWith fadeOut(tween(300)) }, label = "t") { txt ->
                            Text(txt, style = MaterialTheme.typography.titleLarge, color = labelColor, textAlign = TextAlign.Center)
                        }
                    }
                    Spacer(Modifier.weight(1f))
                }
            } else {
                val tr = DemoTracks[resultIdx]
                Column(Modifier.fillMaxSize().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                    ArtCover(tr.art, Modifier.size(height * 0.38f), RoundedCornerShape(28.dp), animated = true)
                    Spacer(Modifier.height(18.dp))
                    Text(tr.title, style = MaterialTheme.typography.headlineSmall, color = cs.onSurface)
                    Text(tr.artist, style = MaterialTheme.typography.titleSmall, color = cs.onSurfaceVariant)
                    Spacer(Modifier.height(16.dp))
                    Button(onClick = { state = 0 }, shapes = ButtonDefaults.shapes()) {
                        Icon(Icons.Rounded.PlayArrow, null)
                        Spacer(Modifier.width(6.dp))
                        Text(s.demoPlay)
                    }
                }
            }
        }
    }
}
