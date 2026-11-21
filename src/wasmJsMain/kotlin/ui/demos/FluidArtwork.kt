package com.alananasss.kittytunewebsite.ui.demos

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.BlurEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.asSkiaBitmap
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.nativeCanvas
import com.alananasss.kittytunewebsite.ui.kit.rememberClock
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.imageResource
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

/*
 * Web port of KittyTune Desktop's FluidArtworkBackground (Apple Music's full-screen background):
 * four copies of the sleeve at √2, 0.8, 0.5 and 0.25 of the longer side, turning in alternate
 * directions, the two small ones orbiting, the field twisted twice, graded (saturate 2.2, brightness
 * 0.6, contrast 0.3) and blurred past recognition. Same SkSL, same numbers.
 */
private const val FLUID_SKSL = """
uniform shader art;
uniform float2 artSize;
uniform float4 sides;
uniform float4 angles;
uniform float2 centre0;
uniform float2 centre1;
uniform float2 centre2;
uniform float2 centre3;
uniform float2 twistA;
uniform float2 twistB;
uniform float twistRadius;
uniform float twistAngle;

float2 twist(float2 coord, float2 offset, float radius, float angle) {
    float2 d = coord - offset;
    float dist = length(d);
    if (dist < radius) {
        float ratio = (radius - dist) / radius;
        float a = ratio * ratio * angle;
        float s = sin(a);
        float c = cos(a);
        d = float2(d.x * c - d.y * s, d.x * s + d.y * c);
    }
    return d + offset;
}

float2 localOf(float2 p, float2 centre, float angle, float side) {
    float2 d = p - centre;
    float c = cos(-angle);
    float s = sin(-angle);
    return float2(d.x * c - d.y * s, d.x * s + d.y * c) / side + 0.5;
}

bool covers(float2 uv) {
    return uv.x >= 0.0 && uv.x <= 1.0 && uv.y >= 0.0 && uv.y <= 1.0;
}

half4 main(float2 fragCoord) {
    float2 p = twist(fragCoord, twistA, twistRadius, twistAngle);
    p = twist(p, twistB, twistRadius, -twistAngle);
    float2 uv = clamp(localOf(p, centre0, angles.x, sides.x), 0.0, 1.0);
    float2 u1 = localOf(p, centre1, angles.y, sides.y);
    float2 u2 = localOf(p, centre2, angles.z, sides.z);
    float2 u3 = localOf(p, centre3, angles.w, sides.w);
    if (covers(u1)) uv = u1;
    if (covers(u2)) uv = u2;
    if (covers(u3)) uv = u3;
    float3 c = float3(art.eval(uv * artSize).rgb);
    float mid = (c.r + c.g + c.b) / 3.0;
    c = float3(mid) + (c - float3(mid)) * 2.2;
    c *= 0.6;
    c = c * 1.3 - float3(0.15);
    return half4(half3(clamp(c, 0.0, 1.0)), 1.0);
}
"""

private val fluidEffect: org.jetbrains.skia.RuntimeEffect? by lazy {
    runCatching { org.jetbrains.skia.RuntimeEffect.makeForShader(FLUID_SKSL) }.getOrNull()
}

private val ROTATION = floatArrayOf(0.06f, -0.12f, 0.06f, -0.08f)
private val PHASES = floatArrayOf(0.4f, 2.1f, 3.7f, 5.2f, 1.3f, 4.4f)
private const val FLOW_SPEED = 2.5f

/** The sleeve itself as a living, blurred backdrop — exactly what the desktop full-screen player shows. */
@Composable
fun FluidArtwork(art: DrawableResource, modifier: Modifier = Modifier, fallback: Color = Color(0xFF0B0B14)) {
    val bitmap = imageResource(art)
    val effect = fluidEffect
    val clock = rememberClock()
    val texture = remember(bitmap) {
        runCatching {
            val img = org.jetbrains.skia.Image.makeFromBitmap(bitmap.asSkiaBitmap())
            img to img.makeShader(org.jetbrains.skia.FilterTileMode.CLAMP, org.jetbrains.skia.FilterTileMode.CLAMP, org.jetbrains.skia.SamplingMode.LINEAR)
        }.getOrNull()
    }
    val builder = remember(effect) { effect?.let { org.jetbrains.skia.RuntimeShaderBuilder(it) } }
    val paint = remember { org.jetbrains.skia.Paint() }
    DisposableEffect(builder) { onDispose { builder?.close() } }

    Box(modifier.clipToBounds().background(fallback)) {
        Box(
            Modifier
                .fillMaxSize()
                .graphicsLayer {
                    val sigma = (minOf(size.width, size.height) * 0.10f).coerceIn(24f, 220f)
                    renderEffect = BlurEffect(sigma, sigma, TileMode.Clamp)
                }
                .drawBehind {
                    val b = builder
                    val tex = texture
                    if (b == null || tex == null) {
                        // No runtime effects on this GPU: a still, graded copy of the cover still reads right.
                        drawImage(bitmap, dstSize = androidx.compose.ui.unit.IntSize(size.width.toInt(), size.height.toInt()), alpha = 0.55f)
                        return@drawBehind
                    }
                    val w = size.width
                    val h = size.height
                    val longest = maxOf(w, h)
                    val t = clock.value * FLOW_SPEED
                    val cx = w / 2f
                    val cy = h / 2f
                    val a3 = 0.045f * t + PHASES[4]
                    val a4 = 0.09f * t + PHASES[5]
                    b.child("art", tex.second)
                    b.uniform("artSize", tex.first.width.toFloat(), tex.first.height.toFloat())
                    b.uniform("sides", longest * sqrt(2f), longest * 0.8f, longest * 0.5f, longest * 0.25f)
                    b.uniform(
                        "angles",
                        PHASES[0] + ROTATION[0] * t, PHASES[1] + ROTATION[1] * t,
                        PHASES[2] + ROTATION[2] * t, PHASES[3] + ROTATION[3] * t,
                    )
                    b.uniform("centre0", cx, cy)
                    b.uniform("centre1", w / 2.5f, h / 2.5f)
                    b.uniform("centre2", cx + w * 0.25f * cos(a3), cy + w * 0.25f * sin(a3))
                    b.uniform("centre3", cx + w * 0.10f * cos(a4), cy + w * 0.10f * sin(a4))
                    b.uniform("twistA", w * 0.25f, h)
                    b.uniform("twistB", w * 0.75f, 0f)
                    b.uniform("twistRadius", (maxOf(w, h) + minOf(w, h)) / 2f)
                    b.uniform("twistAngle", 1.6f)
                    val shader = runCatching { b.makeShader(null) }.getOrNull() ?: return@drawBehind
                    paint.shader = shader
                    drawIntoCanvas { c -> c.nativeCanvas.drawRect(org.jetbrains.skia.Rect.makeWH(w, h), paint) }
                }
        )
    }
}
