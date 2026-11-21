package com.alananasss.kittytunewebsite.ui.lyrics

/*
 * Web port of KittyTune Desktop's lyrics engine (ui/player/lyrics/accompanist — Apple Music parity,
 * itself built on Mocharealm Accompanist). Same layout maths, same per-syllable gradient fill,
 * same per-character DipAndRise / Swell / Bounce animation on held words, same staggered springs.
 */

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.EaseInOut
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.BlurEffect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import kotlin.math.abs

// ───────────────────────────────── model ─────────────────────────────────

data class KSyllable(val content: String, val start: Int, val end: Int) {
    fun progress(t: Int): Float = ((t - start).toFloat() / (end - start).coerceAtLeast(1)).coerceIn(0f, 1f)
}

data class KLine(val syllables: List<KSyllable>, val translation: String? = null, val alignEnd: Boolean = false) {
    val start: Int get() = syllables.first().start
    val end: Int get() = syllables.last().end
}

/**
 * Builds a word-timed line from a compact script: `"Hold|800 on|1600"`. A token is `text|ms`;
 * `text-|ms` is a syllable that continues the same word (no space after it). Default 280 ms.
 */
fun scriptLine(startMs: Int, script: String, translation: String? = null, alignEnd: Boolean = false): KLine {
    var t = startMs
    val tokens = script.trim().split(Regex("\\s+"))
    val out = tokens.mapIndexed { i, tok ->
        val parts = tok.split('|')
        val raw = parts[0]
        val ms = parts.getOrNull(1)?.toIntOrNull() ?: 280
        val continues = raw.endsWith("-")
        val text = raw.removeSuffix("-").replace('_', ' ')
        val content = if (continues || i == tokens.lastIndex) text else "$text "
        KSyllable(content, t, t + ms).also { t += ms }
    }
    return KLine(out, translation, alignEnd)
}

// ───────────────────────────────── easing (Accompanist) ─────────────────────────────────

private class NewtonEasing(vararg points: Pair<Double, Double>) : Easing {
    private val xs = points.map { it.first }
    private val dd: List<Double>

    init {
        val n = points.size
        val table = Array(n) { DoubleArray(n) }
        for (i in 0 until n) table[i][0] = points[i].second
        for (j in 1 until n) for (i in j until n) table[i][j] = (table[i][j - 1] - table[i - 1][j - 1]) / (xs[i] - xs[i - j])
        dd = List(n) { table[it][it] }
    }

    override fun transform(fraction: Float): Float {
        val x = fraction.toDouble()
        var r = dd[xs.lastIndex]
        for (i in xs.lastIndex - 1 downTo 0) r = r * (x - xs[i]) + dd[i]
        return r.toFloat()
    }
}

private fun dipAndRise(dip: Double) = NewtonEasing(0.0 to 0.0, 0.5 to -dip, 1.0 to 1.0)
private fun swell(s: Double) = NewtonEasing(0.0 to 0.0, 0.5 to s, 1.0 to 0.0)
private val Bounce = NewtonEasing(0.0 to 0.0, 0.7 to 1.0, 1.0 to 0.0)
private val SimpleFloatEasing = CubicBezierEasing(0.0f, 0.0f, 0.2f, 1.0f)
private const val FixedSimpleAnimationDurationMs = 700f
private const val MaxSimpleFloatOffsetY = 4f
private const val SOFT_EDGE_PX = 100f
private val LayerPaint = Paint()

// ───────────────────────────────── layout ─────────────────────────────────

private class WordAnim(val start: Long, val end: Long, val content: String) {
    val duration = end - start
}

private data class SylLayout(
    val syl: KSyllable,
    val layout: TextLayoutResult,
    val wordId: Int,
    val awesome: Boolean,
    val width: Float,
    val position: Offset = Offset.Zero,
    val wordPivot: Offset = Offset.Zero,
    val wordAnim: WordAnim? = null,
    val charOffsetInWord: Int = 0,
    val charLayouts: List<TextLayoutResult>? = null,
    val charBounds: List<Rect>? = null,
    val baseline: Float = layout.firstBaseline,
)

private class Row(val items: List<SylLayout>, val minX: Float, val maxX: Float, val bounds: Rect) {
    val width = maxX - minX
    val first = items.first().syl.start
    val last = items.last().syl.end
}

private class LineLayout(val rows: List<Row>, val height: Float, val translation: TextLayoutResult?, val translationTop: Float)

private fun groupWords(s: List<KSyllable>): List<List<KSyllable>> {
    val words = mutableListOf<List<KSyllable>>()
    var cur = mutableListOf<KSyllable>()
    s.forEach { syl ->
        cur += syl
        if (syl.content.trimEnd().length < syl.content.length) { words += cur; cur = mutableListOf() }
    }
    if (cur.isNotEmpty()) words += cur
    return words
}

private fun layoutLine(line: KLine, tm: TextMeasurer, style: TextStyle, tStyle: TextStyle, widthPx: Float, density: Float): LineLayout {
    val spaceW = tm.measure(" ", style).size.width.toFloat()
    // measure syllables, decide which words get the per-character "awesome" animation
    val measured = groupWords(line.syllables).flatMapIndexed { wi, word ->
        val content = word.joinToString("") { it.content }
        val dur = word.last().end - word.first().start
        val perChar = if (content.isNotEmpty() && dur > 0) dur.toFloat() / content.length else 0f
        val awesome = perChar > 200f && dur >= 1000
        word.map { syl ->
            val lr = tm.measure(syl.content, style)
            var w = lr.size.width.toFloat()
            if (syl.content.endsWith(" ")) {
                val trimmed = tm.measure(syl.content.trimEnd(), style).size.width.toFloat()
                if (w <= trimmed) w = trimmed + spaceW * (syl.content.length - syl.content.trimEnd().length)
            }
            val chars = if (awesome) syl.content.map { tm.measure(it.toString(), style) } else null
            val bounds = if (awesome) syl.content.indices.map { lr.getBoundingBox(it) } else null
            SylLayout(syl, lr, wi, awesome, w, charLayouts = chars, charBounds = bounds)
        }
    }
    // greedy word wrap
    val rowsRaw = mutableListOf<MutableList<SylLayout>>()
    var cur = mutableListOf<SylLayout>()
    var curW = 0f
    measured.groupBy { it.wordId }.values.forEach { word ->
        val ww = word.sumOf { it.width.toDouble() }.toFloat()
        if (curW + ww > widthPx && cur.isNotEmpty()) { rowsRaw += cur; cur = mutableListOf(); curW = 0f }
        cur.addAll(word); curW += ww
    }
    if (cur.isNotEmpty()) rowsRaw += cur
    // trim trailing spaces per row
    val rowsTrimmed = rowsRaw.map { r ->
        val last = r.last()
        val t = last.syl.content.trimEnd()
        if (t.length < last.syl.content.length && t.isNotEmpty()) {
            val lr = tm.measure(t, style)
            r.dropLast(1) + last.copy(syl = last.syl.copy(content = t), layout = lr, width = lr.size.width.toFloat())
        } else r
    }
    val lineH = tm.measure("M", style).size.height.toFloat()
    // position
    val byWord = mutableMapOf<Int, MutableList<SylLayout>>()
    val positioned = rowsTrimmed.mapIndexed { ri, r ->
        val total = r.sumOf { it.width.toDouble() }.toFloat()
        var x = if (line.alignEnd) (widthPx - total).coerceAtLeast(0f) else 0f
        val maxBase = r.maxOf { it.baseline }
        r.map { l ->
            val p = l.copy(position = Offset(x, ri * lineH + (maxBase - l.baseline)))
            x += l.width
            byWord.getOrPut(p.wordId) { mutableListOf() }.add(p)
            p
        }
    }
    val anims = mutableMapOf<Int, WordAnim>()
    val charOffsets = mutableMapOf<SylLayout, Int>()
    byWord.forEach { (id, ls) ->
        if (ls.first().awesome) {
            anims[id] = WordAnim(ls.minOf { it.syl.start }.toLong(), ls.maxOf { it.syl.end }.toLong(), ls.joinToString("") { it.syl.content })
            var run = 0
            ls.forEach { charOffsets[it] = run; run += it.syl.content.length }
        }
    }
    val finalRows = positioned.map { r ->
        val items = r.map { p ->
            val ws = byWord.getValue(p.wordId)
            val minX = ws.minOf { it.position.x }
            val maxX = ws.maxOf { it.position.x + it.width }
            val bottom = ws.maxOf { it.position.y + it.layout.size.height }
            p.copy(wordPivot = Offset((minX + maxX) / 2f, bottom), wordAnim = anims[p.wordId], charOffsetInWord = charOffsets[p] ?: 0)
        }
        val minX = items.minOf { it.position.x }
        val maxX = items.maxOf { it.position.x + it.width }
        val minY = items.minOf { it.position.y }
        val h = items.maxOf { it.layout.size.height }.toFloat()
        val vp = h * 0.1f * density
        val hp = (maxX - minX) * 0.2f * density
        val edge = 8f * density
        Row(items, minX, maxX, Rect(minX - hp, minY - vp - edge, maxX + hp, minY + h + vp + edge))
    }
    val textH = lineH * finalRows.size
    val tr = line.translation?.let {
        tm.measure(it, tStyle.copy(textAlign = if (line.alignEnd) TextAlign.End else TextAlign.Start), constraints = Constraints(maxWidth = widthPx.toInt().coerceAtLeast(1)))
    }
    val trTop = textH + 4f * density
    val height = textH + (tr?.let { 4f * density + it.size.height } ?: 0f)
    return LineLayout(finalRows, height, tr, trTop)
}

// ───────────────────────────────── drawing (KaraokeLineText) ─────────────────────────────────

private fun gradientBrush(row: Row, t: Int): Brush {
    val active = Color.White
    val inactive = Color.White.copy(alpha = 0.2f)
    if (row.width <= 0f) return SolidColor(if (t >= row.last) active else inactive)
    if (t <= row.first) return SolidColor(inactive)
    if (t >= row.last) return SolidColor(active)
    val cur = row.items.find { t in it.syl.start until it.syl.end }
    val playX: Float
    val edge: Float
    if (cur != null) {
        val sung = cur.width * cur.syl.progress(t)
        val left = cur.width - sung
        playX = cur.position.x + sung
        edge = minOf(SOFT_EDGE_PX, sung * 2f, left * 2f)
    } else {
        val done = row.items.lastOrNull { t >= it.syl.end }
        playX = done?.let { it.position.x + it.width } ?: row.minX
        edge = 0f
    }
    val at = ((playX - row.minX) / row.width).coerceIn(0f, 1f)
    val half = edge / 2f / row.width
    return Brush.horizontalGradient(
        0f to active, (at - half).coerceIn(0f, 1f) to active, (at + half).coerceIn(0f, 1f) to inactive, 1f to inactive,
        startX = row.minX, endX = row.maxX,
    )
}

private fun DrawScope.drawRowText(row: Row, color: Color, t: Int) {
    row.items.forEachIndexed { index, sl ->
        val wa = sl.wordAnim
        if (wa != null) {
            val awesomeDuration = wa.duration * 0.8f
            val chars = sl.charLayouts ?: emptyList()
            val bounds = sl.charBounds ?: emptyList()
            val n = wa.content.length
            val earliest = wa.start
            val latest = wa.end - awesomeDuration
            val intensity = (wa.duration - 200f * n) / 1000.0
            val dr = dipAndRise((0.5 * intensity).coerceIn(0.0, 0.5))
            val sw = swell((0.1 * intensity).coerceIn(0.0, 0.1))
            sl.syl.content.forEachIndexed { ci, _ ->
                val cl = chars.getOrNull(ci) ?: return@forEachIndexed
                val box = bounds.getOrNull(ci) ?: return@forEachIndexed
                val abs = sl.charOffsetInWord + ci
                val ratio = if (n > 1) abs.toFloat() / (n - 1) else 0.5f
                val st = (earliest + (latest - earliest) * ratio).toLong()
                val p = ((t - st).toFloat() / awesomeDuration).coerceIn(0f, 1f)
                val floatOffset = 4f * dr.transform(1f - p)
                val sc = 1f + sw.transform(p)
                val x = sl.position.x + box.left + (box.width - cl.size.width) / 2f
                val y = sl.position.y + (sl.baseline - cl.firstBaseline) + floatOffset
                val shadow = Shadow(color.copy(0.4f), Offset.Zero, 10f * Bounce.transform(p))
                withTransform({ scale(sc, sc, sl.wordPivot) }) {
                    drawText(cl, color = color, topLeft = Offset(x, y), shadow = shadow)
                }
            }
        } else {
            val driver = if (sl.syl.content.trim().all { !it.isLetterOrDigit() } && index > 0) row.items[index - 1] else sl
            val p = ((t - driver.syl.start) / FixedSimpleAnimationDurationMs).coerceIn(0f, 1f)
            val off = MaxSimpleFloatOffsetY * SimpleFloatEasing.transform(1f - p)
            drawText(sl.layout, color = color, topLeft = sl.position.copy(y = sl.position.y + off))
        }
    }
}

private fun DrawScope.drawLine(ll: LineLayout, t: Int, color: Color) {
    ll.rows.forEach { row ->
        if (t >= row.last) {
            drawRowText(row, color, t)
            return@forEach
        }
        drawIntoCanvas { canvas ->
            canvas.saveLayer(row.bounds, LayerPaint)
            drawRowText(row, color, t)
            drawRect(gradientBrush(row, t), row.bounds.topLeft, row.bounds.size, blendMode = BlendMode.DstIn)
            canvas.restore()
        }
    }
    ll.translation?.let { drawText(it, color = color.copy(alpha = 0.4f), topLeft = Offset(0f, ll.translationTop)) }
}

// ───────────────────────────────── view (KaraokeLyricsView) ─────────────────────────────────

private const val LINE_STIFFNESS = 300f
private const val LINE_STIFFNESS_STEP = 18f
private const val LINE_STIFFNESS_MIN = 190f

/**
 * Scrolling karaoke view. The sung line sits at [anchorFraction] of the height; every other line
 * springs after it with a slightly softer spring the farther it is, and blurs by distance.
 */
@Composable
fun KaraokeLyricsView(
    lines: List<KLine>,
    timeMs: () -> Int,
    modifier: Modifier = Modifier,
    textStyle: TextStyle,
    color: Color = Color.White,
    anchorFraction: Float = 0.3f,
    lineSpacing: androidx.compose.ui.unit.Dp = 10.dp,
    blurDelta: Float = 3f,
    activeScale: Float = 1.06f,
) {
    val tm = rememberTextMeasurer(cacheSize = 64)
    val density = LocalDensity.current
    BoxWithConstraints(
        modifier
            .clipToBounds()
            .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
            .drawWithContent {
                drawContent()
                val top = 20.dp.toPx() / size.height
                val bottom = 80.dp.toPx() / size.height
                drawRect(
                    Brush.verticalGradient(0f to Color.Transparent, top to Color.Black, 1f - bottom to Color.Black, 1f to Color.Transparent),
                    blendMode = BlendMode.DstIn
                )
            }
    ) {
        val boxW = maxWidth
        val widthPx = with(density) { boxW.toPx() } / activeScale
        val heightPx = with(density) { maxHeight.toPx() }
        val padV = with(density) { 8.dp.toPx() }
        val gap = with(density) { lineSpacing.toPx() }
        val tStyle = remember(textStyle) { textStyle.copy(fontSize = textStyle.fontSize * 0.5f, lineHeight = textStyle.fontSize * 0.65f, fontWeight = androidx.compose.ui.text.font.FontWeight.W600) }
        val layouts = remember(lines, widthPx, textStyle) { lines.map { layoutLine(it, tm, textStyle, tStyle, widthPx, density.density) } }
        val tops = remember(layouts) {
            var y = 0f
            layouts.map { l -> y.also { y += l.height + padV * 2 + gap } }
        }

        val active by remember(lines) {
            derivedStateOf {
                val t = timeMs()
                lines.indices.findLast { t >= lines[it].start && t < lines[it].end }
                    ?: lines.indexOfFirst { it.start > t }.takeIf { it >= 0 }
                    ?: lines.lastIndex
            }
        }
        val anims = remember(lines) { lines.indices.map { Animatable(0f) } }
        val firstPlacement = remember(lines) { booleanArrayOf(true) }
        LaunchedEffect(active, tops) {
            val target = tops[active] - heightPx * anchorFraction
            if (firstPlacement[0]) {
                firstPlacement[0] = false
                anims.forEach { it.snapTo(target) }
                return@LaunchedEffect
            }
            anims.forEachIndexed { i, a ->
                val dist = abs(i - active)
                val stiffness = (LINE_STIFFNESS - dist * LINE_STIFFNESS_STEP).coerceAtLeast(LINE_STIFFNESS_MIN)
                // A wrap back to line one would fly through the whole song: jump instead.
                if (abs(a.value - target) > heightPx * 1.5f) launch { a.snapTo(target) }
                else launch { a.animateTo(target, spring(dampingRatio = 0.95f, stiffness = stiffness)) }
            }
        }

        lines.indices.forEach { i ->
            val ll = layouts[i]
            val focused = i == active
            val dist = abs(i - active)
            val scale by androidx.compose.animation.core.animateFloatAsState(
                if (focused) activeScale else 0.98f,
                if (focused) tween(600, easing = LinearOutSlowInEasing) else tween(300, easing = EaseInOut)
            )
            val alpha by androidx.compose.animation.core.animateFloatAsState(if (focused) 1f else 0.4f)
            val blur by androidx.compose.animation.core.animateFloatAsState(dist * blurDelta, tween(600))
            val hDp = with(density) { (ll.height + padV * 2).toDp() }
            Box(
                Modifier
                    .size(boxW, hDp)
                    .graphicsLayer {
                        translationY = tops[i] - anims[i].value
                        scaleX = scale; scaleY = scale
                        this.alpha = alpha
                        transformOrigin = TransformOrigin(if (lines[i].alignEnd) 1f else 0f, 1f)
                        renderEffect = if (blur > 0.3f) BlurEffect(blur, blur, TileMode.Decal) else null
                    }
            ) {
                Canvas(Modifier.size(boxW, hDp)) {
                    // Only the visible band draws; lines far away are skipped entirely.
                    val y = tops[i] - anims[i].value
                    if (y > heightPx + 200f || y + size.height < -200f) return@Canvas
                    withTransform({ translate(0f, padV) }) { drawLine(ll, timeMs(), color) }
                }
            }
        }
    }
}
