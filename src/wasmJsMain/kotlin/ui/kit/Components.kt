@file:OptIn(ExperimentalMaterial3ExpressiveApi::class)

package com.alananasss.kittytunewebsite.ui.kit

import androidx.compose.foundation.Image
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.VerticalScrollbar
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.rememberScrollbarAdapter
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.graphics.shapes.RoundedPolygon
import kittytunewebsite.generated.resources.Res
import kittytunewebsite.generated.resources.kitty_logo
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource

/** Space reserved by the floating app chrome (top bar / bottom toolbar) that pages must not draw under. */
data class ChromeInsets(val top: Dp = 0.dp, val bottom: Dp = 0.dp)

val LocalChromeInsets = compositionLocalOf { ChromeInsets() }

const val MaxContentWidth = 1240

/** Centers its content and caps it at [MaxContentWidth] with responsive side gutters. */
@Composable
fun ContentWidth(
    modifier: Modifier = Modifier,
    maxWidth: Dp = MaxContentWidth.dp,
    content: @Composable BoxWithConstraintsScope.() -> Unit,
) {
    BoxWithConstraints(modifier.fillMaxWidth(), contentAlignment = Alignment.TopCenter) {
        val gutter = when {
            this.maxWidth < 600.dp -> 16.dp
            this.maxWidth < 1000.dp -> 32.dp
            else -> 48.dp
        }
        BoxWithConstraints(
            Modifier.padding(horizontal = gutter).widthIn(max = maxWidth).fillMaxWidth(),
            content = content
        )
    }
}

/** Page scaffold: a scrolling column offset below the floating chrome, with a desktop scrollbar. */
@Composable
fun PageColumn(
    modifier: Modifier = Modifier,
    scrollState: ScrollState = rememberScrollState(),
    spacing: Dp = 0.dp,
    content: @Composable ColumnScope.() -> Unit,
) {
    val insets = LocalChromeInsets.current
    BoxWithConstraints(modifier.fillMaxSize()) {
        val isMobile = maxWidth < 600.dp
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(top = insets.top, bottom = insets.bottom),
            verticalArrangement = Arrangement.spacedBy(spacing),
            content = content,
        )
        if (!isMobile) {
            VerticalScrollbar(
                modifier = Modifier.align(Alignment.CenterEnd).fillMaxHeight().padding(top = insets.top, bottom = 4.dp, end = 2.dp),
                adapter = rememberScrollbarAdapter(scrollState = scrollState)
            )
        }
    }
}

/**
 * Lazy page scaffold: only the sections on (or near) screen are composed, measured and drawn. The
 * old scrolling Column re-measured every section of the page on each tab switch.
 */
@Composable
fun LazyPageColumn(
    state: androidx.compose.foundation.lazy.LazyListState,
    modifier: Modifier = Modifier,
    content: androidx.compose.foundation.lazy.LazyListScope.() -> Unit,
) {
    val insets = LocalChromeInsets.current
    BoxWithConstraints(modifier.fillMaxSize()) {
        val isMobile = maxWidth < 600.dp
        androidx.compose.foundation.lazy.LazyColumn(
            state = state,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(top = insets.top, bottom = insets.bottom),
            content = content,
        )
        if (!isMobile) {
            VerticalScrollbar(
                modifier = Modifier.align(Alignment.CenterEnd).fillMaxHeight().padding(top = insets.top, bottom = 4.dp, end = 2.dp),
                adapter = rememberScrollbarAdapter(scrollState = state)
            )
        }
    }
}

@Composable
fun Eyebrow(
    text: String,
    icon: ImageVector? = null,
    container: Color = MaterialTheme.colorScheme.secondaryContainer,
    content: Color = MaterialTheme.colorScheme.onSecondaryContainer,
    modifier: Modifier = Modifier,
) {
    Surface(shape = CircleShape, color = container, modifier = modifier) {
        Row(
            Modifier.padding(start = if (icon != null) 10.dp else 16.dp, end = 16.dp, top = 8.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (icon != null) Icon(icon, null, tint = content, modifier = Modifier.size(18.dp))
            Text(text, style = MaterialTheme.typography.labelLarge, color = content)
        }
    }
}

@Composable
fun SectionHeading(
    eyebrow: String?,
    title: String,
    subtitle: String? = null,
    modifier: Modifier = Modifier,
    centered: Boolean = false,
    eyebrowIcon: ImageVector? = null,
) {
    BoxWithConstraints(modifier) {
        val compact = maxWidth < 640.dp
        Column(
            verticalArrangement = Arrangement.spacedBy(if (compact) 14.dp else 18.dp),
            horizontalAlignment = if (centered) Alignment.CenterHorizontally else Alignment.Start,
            modifier = Modifier.fillMaxWidth()
        ) {
            if (eyebrow != null) Eyebrow(eyebrow, eyebrowIcon)
            Text(
                title,
                style = if (compact) MaterialTheme.typography.displaySmall else MaterialTheme.typography.displayMedium,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = if (centered) TextAlign.Center else TextAlign.Start,
            )
            if (subtitle != null) {
                Text(
                    subtitle,
                    style = if (compact) MaterialTheme.typography.bodyLarge else MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.W500),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = if (centered) TextAlign.Center else TextAlign.Start,
                    modifier = Modifier.widthIn(max = 760.dp)
                )
            }
        }
    }
}

/** An icon sitting inside an expressive shape. */
@Composable
fun ShapeIcon(
    icon: ImageVector,
    polygon: RoundedPolygon = MaterialShapes.Cookie9Sided,
    size: Dp = 56.dp,
    container: Color = MaterialTheme.colorScheme.primaryContainer,
    content: Color = MaterialTheme.colorScheme.onPrimaryContainer,
    rotation: Float = 0f,
) {
    ShapeBox(polygon, Modifier.size(size).background(container), rotation = rotation) {
        Icon(icon, null, tint = content, modifier = Modifier.size(size * 0.46f))
    }
}

/** Large rounded tile used in bento grids. Hover lifts it, press squishes it. */
@Composable
fun BentoCard(
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.surfaceContainer,
    shape: Shape = RoundedCornerShape(36.dp),
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    val source = remember { MutableInteractionSource() }
    Surface(
        modifier = modifier
            .springyPress(source, pressed = 0.98f, hovered = 1.012f)
            .then(if (onClick != null) Modifier.clip(shape).clickable(source, null, onClick = onClick) else Modifier.hoverable(source)),
        shape = shape,
        color = color,
    ) {
        Column(content = content)
    }
}

/** Small label/value pill. */
@Composable
fun TagPill(text: String, color: Color = MaterialTheme.colorScheme.surfaceContainerHighest, content: Color = MaterialTheme.colorScheme.onSurfaceVariant, icon: ImageVector? = null) {
    Surface(shape = CircleShape, color = color) {
        Row(
            Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            if (icon != null) Icon(icon, null, tint = content, modifier = Modifier.size(14.dp))
            Text(text, style = MaterialTheme.typography.labelMedium, color = content, maxLines = 1)
        }
    }
}

/** Wrapping row layout (a tiny FlowRow that stays stable across Compose versions). */
@Composable
fun Wrap(
    modifier: Modifier = Modifier,
    hSpacing: Dp = 8.dp,
    vSpacing: Dp = 8.dp,
    center: Boolean = false,
    content: @Composable () -> Unit,
) {
    Layout(content, modifier) { measurables, constraints ->
        val hs = hSpacing.roundToPx()
        val vs = vSpacing.roundToPx()
        val loose = constraints.copy(minWidth = 0, minHeight = 0)
        val placeables = measurables.map { it.measure(loose) }
        val maxW = if (constraints.hasBoundedWidth) constraints.maxWidth else Int.MAX_VALUE
        val rows = mutableListOf<MutableList<androidx.compose.ui.layout.Placeable>>()
        var cur = mutableListOf<androidx.compose.ui.layout.Placeable>()
        var w = 0
        for (p in placeables) {
            if (cur.isNotEmpty() && w + hs + p.width > maxW) {
                rows += cur; cur = mutableListOf(); w = 0
            }
            w += (if (cur.isEmpty()) 0 else hs) + p.width
            cur += p
        }
        if (cur.isNotEmpty()) rows += cur
        val rowWidths = rows.map { r -> r.sumOf { it.width } + hs * (r.size - 1) }
        val rowHeights = rows.map { r -> r.maxOf { it.height } }
        val width = if (constraints.hasBoundedWidth) constraints.maxWidth else (rowWidths.maxOrNull() ?: 0)
        val height = rowHeights.sum() + vs * (rows.size - 1).coerceAtLeast(0)
        layout(width, height.coerceAtLeast(0)) {
            var y = 0
            rows.forEachIndexed { i, r ->
                var x = if (center) (width - rowWidths[i]) / 2 else 0
                r.forEach { p ->
                    p.placeRelative(x, y + (rowHeights[i] - p.height) / 2)
                    x += p.width + hs
                }
                y += rowHeights[i] + vs
            }
        }
    }
}

/** The KittyTune pixel cat, tinted to the theme. */
@Composable
fun KittyLogo(modifier: Modifier = Modifier, tint: Color = MaterialTheme.colorScheme.primary) {
    Image(
        painter = painterResource(Res.drawable.kitty_logo),
        contentDescription = "KittyTune",
        colorFilter = ColorFilter.tint(tint),
        contentScale = ContentScale.Fit,
        modifier = modifier,
    )
}

/** A real screenshot, decoded by Skia straight into the canvas (no DOM overlay). */
@Composable
fun Screenshot(
    res: DrawableResource,
    description: String,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Fit,
) {
    Image(
        painter = painterResource(res),
        contentDescription = description,
        contentScale = contentScale,
        modifier = modifier,
    )
}

/** Fades the left and right edges to transparent, so horizontally scrolling rows dissolve instead of being cut. */
fun Modifier.fadingEdges(edge: Dp = 48.dp): Modifier = this
    .graphicsLayer { compositingStrategy = androidx.compose.ui.graphics.CompositingStrategy.Offscreen }
    .drawWithContent {
        drawContent()
        val f = (edge.toPx() / size.width).coerceIn(0f, 0.5f)
        drawRect(
            androidx.compose.ui.graphics.Brush.horizontalGradient(0f to Color.Transparent, f to Color.Black, 1f - f to Color.Black, 1f to Color.Transparent),
            blendMode = androidx.compose.ui.graphics.BlendMode.DstIn
        )
    }

/** Endless horizontal ticker. */
@Composable
fun Marquee(
    modifier: Modifier = Modifier,
    speedDpPerSecond: Float = 42f,
    reverse: Boolean = false,
    content: @Composable () -> Unit,
) {
    val clock = rememberClock()
    var contentWidth by remember { mutableIntStateOf(0) }
    Layout(
        content = { content(); content() },
        modifier = modifier.clip(RoundedCornerShape(0.dp)).graphicsLayer { clip = true },
    ) { measurables, constraints ->
        val loose = constraints.copy(minWidth = 0, maxWidth = Int.MAX_VALUE)
        val ps = measurables.map { it.measure(loose) }
        val half = ps.take(ps.size / 2).sumOf { it.width }
        contentWidth = half
        val h = ps.maxOfOrNull { it.height } ?: 0
        val w = if (constraints.hasBoundedWidth) constraints.maxWidth else half
        val speedPx = speedDpPerSecond.dp.toPx()
        layout(w, h) {
            // Placed once; the scroll is a layer translation read in the draw phase, so the ticker
            // never relayouts.
            var x = if (reverse) -half else 0
            ps.forEach { p ->
                p.placeRelativeWithLayer(x, (h - p.height) / 2) {
                    val shift = if (half > 0) ((clock.value * speedPx) % half) else 0f
                    translationX = if (reverse) shift else -shift
                }
                x += p.width
            }
        }
    }
}
