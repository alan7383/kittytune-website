@file:OptIn(ExperimentalMaterial3ExpressiveApi::class)

package com.alananasss.kittytunewebsite.ui.kit

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialShapes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.graphics.shapes.Morph
import androidx.graphics.shapes.RoundedPolygon

/** The expressive shape set used across the site, in the order they cycle through. */
val ExpressiveShapes: List<RoundedPolygon> by lazy {
    listOf(
        MaterialShapes.Cookie9Sided,
        MaterialShapes.Sunny,
        MaterialShapes.Clover4Leaf,
        MaterialShapes.SoftBurst,
        MaterialShapes.Cookie12Sided,
        MaterialShapes.Puffy,
        MaterialShapes.Gem,
        MaterialShapes.Flower,
        MaterialShapes.Pill,
        MaterialShapes.Ghostish,
    )
}

private fun Morph.fillPath(progress: Float, size: Size, path: Path = Path()): Path {
    path.rewind()
    val cubics = asCubics(progress)
    if (cubics.isEmpty()) return path
    val sx = size.width
    val sy = size.height
    path.moveTo(cubics.first().anchor0X * sx, cubics.first().anchor0Y * sy)
    for (c in cubics) {
        path.cubicTo(
            c.control0X * sx, c.control0Y * sy,
            c.control1X * sx, c.control1Y * sy,
            c.anchor1X * sx, c.anchor1Y * sy,
        )
    }
    path.close()
    return path
}

/** Straight from the polygon's own cubics: building a Morph(this, this) per outline was the
 *  single most expensive thing in a frame (it re-measures and re-matches both shapes). */
private fun RoundedPolygon.fillPath(size: Size, path: Path = Path()): Path {
    path.rewind()
    val cs = cubics
    if (cs.isEmpty()) return path
    val sx = size.width
    val sy = size.height
    path.moveTo(cs.first().anchor0X * sx, cs.first().anchor0Y * sy)
    for (c in cs) path.cubicTo(c.control0X * sx, c.control0Y * sy, c.control1X * sx, c.control1Y * sy, c.anchor1X * sx, c.anchor1Y * sy)
    path.close()
    return path
}

private val morphCache = HashMap<Pair<RoundedPolygon, RoundedPolygon>, Morph>()

/** Morphs are expensive to build and immutable: one per shape pair for the whole site. */
fun cachedMorph(a: RoundedPolygon, b: RoundedPolygon): Morph = morphCache.getOrPut(a to b) { Morph(a, b) }

private fun cubicPoint(a0: Float, c0: Float, c1: Float, a1: Float, t: Float): Float {
    val u = 1f - t
    return u * u * u * a0 + 3f * u * u * t * c0 + 3f * u * t * t * c1 + t * t * t * a1
}

private val radiusCache = HashMap<RoundedPolygon, Float>()

/** Farthest point of the outline from the polygon's centre, sampled along every cubic. */
private fun RoundedPolygon.maxRadius(): Float = radiusCache.getOrPut(this) {
    var r = 0f
    for (c in cubics) {
        for (k in 0..12) {
            val t = k / 12f
            val x = cubicPoint(c.anchor0X, c.control0X, c.control1X, c.anchor1X, t) - centerX
            val y = cubicPoint(c.anchor0Y, c.control0Y, c.control1Y, c.anchor1Y, t) - centerY
            r = maxOf(r, kotlin.math.sqrt(x * x + y * y))
        }
    }
    r.takeIf { it > 0f } ?: 0.5f
}

/**
 * Builds the outline scaled so its circumscribed circle fits the box, then rotates it. Unlike
 * stretching the polygon to the box, no rotation can push a lobe past the edges, so the clip never
 * slices a corner off (Clover, Gem, Pill… used to lose their tips while turning).
 */
private fun RoundedPolygon.spinSafePath(size: Size, rotation: Float, path: Path = Path()): Path {
    path.rewind()
    val k = minOf(size.width, size.height) / 2f / maxRadius()
    val cx = centerX
    val cy = centerY
    val ox = size.width / 2f
    val oy = size.height / 2f
    val rad = rotation / 180f * kotlin.math.PI.toFloat()
    val cosA = kotlin.math.cos(rad)
    val sinA = kotlin.math.sin(rad)
    fun mx(x: Float, y: Float): Float { val dx = (x - cx) * k; val dy = (y - cy) * k; return ox + dx * cosA - dy * sinA }
    fun my(x: Float, y: Float): Float { val dx = (x - cx) * k; val dy = (y - cy) * k; return oy + dx * sinA + dy * cosA }
    val cs = cubics
    if (cs.isEmpty()) return path
    path.moveTo(mx(cs.first().anchor0X, cs.first().anchor0Y), my(cs.first().anchor0X, cs.first().anchor0Y))
    for (c in cs) {
        path.cubicTo(
            mx(c.control0X, c.control0Y), my(c.control0X, c.control0Y),
            mx(c.control1X, c.control1Y), my(c.control1X, c.control1Y),
            mx(c.anchor1X, c.anchor1Y), my(c.anchor1X, c.anchor1Y),
        )
    }
    path.close()
    return path
}

/** A [Shape] that is a [Morph] frozen at [progress], optionally rotated about its centre. */
class MorphShape(private val morph: Morph, private val progress: Float, private val rotation: Float = 0f) : Shape {
    override fun createOutline(size: Size, layoutDirection: LayoutDirection, density: Density): Outline {
        val p = morph.fillPath(progress, size)
        if (rotation != 0f) {
            // shrink a touch so the rotated outline stays inside the clip bounds
            val m = androidx.compose.ui.graphics.Matrix()
            m.translate(size.width / 2f, size.height / 2f)
            m.rotateZ(rotation)
            m.scale(0.86f, 0.86f)
            m.translate(-size.width / 2f, -size.height / 2f)
            p.transform(m)
        }
        return Outline.Generic(p)
    }
}

/**
 * An expressive polygon as a clip shape. With [spinSafe] (default whenever it is rotated) the outline
 * keeps one constant size at every angle and never crosses the box edges.
 */
class PolygonShape(
    private val polygon: RoundedPolygon,
    private val rotation: Float = 0f,
    private val spinSafe: Boolean = rotation != 0f,
) : Shape {
    override fun createOutline(size: Size, layoutDirection: LayoutDirection, density: Density): Outline {
        val p = if (spinSafe) polygon.spinSafePath(size, rotation) else polygon.fillPath(size)
        return Outline.Generic(p)
    }
}

/** Remembers one [Morph] per consecutive pair in [shapes] (wrapping around). */
@Composable
fun rememberMorphCycle(shapes: List<RoundedPolygon>): List<Morph> = remember(shapes) {
    shapes.indices.map { i -> cachedMorph(shapes[i], shapes[(i + 1) % shapes.size]) }
}

/**
 * Draws a continuously morphing, slowly spinning expressive shape. Animation is driven by the
 * [rememberClock] gate, so it costs nothing while scrolled away.
 */
@Composable
fun MorphingBlob(
    modifier: Modifier = Modifier,
    shapes: List<RoundedPolygon> = ExpressiveShapes,
    brush: Brush? = null,
    color: Color = Color.Unspecified,
    holdSeconds: Float = 1.6f,
    morphSeconds: Float = 0.9f,
    spinDegPerSecond: Float = 14f,
    phase: Float = 0f,
) {
    val morphs = rememberMorphCycle(shapes)
    val clock = rememberClock()
    val path = remember { Path() }
    Canvas(modifier) {
        val t = clock.value + phase * (holdSeconds + morphSeconds)
        val step = holdSeconds + morphSeconds
        val idx = ((t / step).toInt()) % morphs.size
        val local = t % step
        val prog = if (local < holdSeconds) 0f else easeInOut((local - holdSeconds) / morphSeconds)
        morphs[idx].fillPath(prog, size, path)
        rotate(t * spinDegPerSecond) {
            if (brush != null) drawPath(path, brush) else drawPath(path, color)
        }
    }
}

/** Static shape drawn as a filled path. */
fun DrawScope.drawPolygon(polygon: RoundedPolygon, color: Color, topLeft: Offset = Offset.Zero, size: Size = this.size, rotation: Float = 0f) {
    val p = polygon.fillPath(size)
    p.translate(topLeft)
    rotate(rotation, pivot = Offset(topLeft.x + size.width / 2f, topLeft.y + size.height / 2f)) { drawPath(p, color) }
}

/**
 * Fills the box with [polygon] turning at [degreesPerSecond]. The angle is read in the draw phase,
 * so a spinning background never recomposes or relayouts its content (the old ShapeBox(rotation =
 * clock.value) recomposed the whole card on every frame).
 */
fun Modifier.spinningPolygon(polygon: RoundedPolygon, color: Color, clock: androidx.compose.runtime.State<Float>, degreesPerSecond: Float): Modifier =
    this.drawBehind { drawPath(polygon.spinSafePath(size, clock.value * degreesPerSecond), color) }

/** A box clipped to an expressive shape. */
@Composable
fun ShapeBox(
    polygon: RoundedPolygon,
    modifier: Modifier = Modifier,
    rotation: Float = 0f,
    spinning: Boolean = rotation != 0f,
    contentAlignment: Alignment = Alignment.Center,
    content: @Composable BoxScope.() -> Unit = {},
) {
    val shape = remember(polygon, rotation, spinning) { PolygonShape(polygon, rotation, spinning) }
    // Clip first so callers can pass size/background in [modifier] and still get the shape.
    Box(Modifier.clip(shape).then(modifier), contentAlignment = contentAlignment, content = content)
}
