package com.alananasss.kittytunewebsite.ui

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.VerticalScrollbar
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.rememberScrollbarAdapter
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import org.jetbrains.compose.resources.decodeToSvgPainter
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.HtmlElementView
import com.alananasss.kittytunewebsite.ui.kit.LocalChromeInsets
import com.alananasss.kittytunewebsite.ui.kit.SectionHeading
import kotlinx.browser.document
import org.w3c.dom.HTMLElement
import org.w3c.dom.HTMLImageElement

val LocalPageAlpha = compositionLocalOf { 1f }
val LocalDialogActive = compositionLocalOf { false }

enum class MediaFit(val cssValue: String) {
    Cover("cover"),
    Contain("contain"),
}

@Composable
fun SectionHeader(
    title: String,
    description: String,
    modifier: Modifier = Modifier,
    centered: Boolean = false
) {
    SectionHeading(eyebrow = null, title = title, subtitle = description, modifier = modifier, centered = centered)
}

/** DOM <img> overlay, used for SVG platform logos (Skia cannot rasterise every SVG feature). */
@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun HtmlImage(src: String, description: String, corner: Dp, fit: MediaFit, modifier: Modifier = Modifier, cssFilter: String = "", cssTransform: String = "") {
    if (src.endsWith(".svg") && src.startsWith("svg/")) {
        // Bundled logos are decoded by Skia straight into the canvas: no DOM overlay to clip, hide or misplace.
        SvgImage("files/$src", description, modifier)
        return
    }
    if (LocalDialogActive.current) {
        Spacer(modifier = modifier)
        return
    }
    val pageAlpha = LocalPageAlpha.current
    HtmlElementView(
        factory = {
            (document.createElement("img") as HTMLImageElement).apply {
                setAttribute("src", src)
                setAttribute("alt", description)
                setAttribute("loading", "lazy")
                setAttribute("decoding", "async")
                setMediaElementStyle(corner, fit, pageAlpha, cssFilter, cssTransform)
            }
        },
        modifier = modifier,
        update = { element ->
            (element.parentElement as? HTMLElement)?.style?.setProperty("pointer-events", "none")
            if (element.getAttribute("src") != src) element.setAttribute("src", src)
            element.setAttribute("alt", description)
            element.setMediaElementStyle(corner, fit, pageAlpha, cssFilter, cssTransform)
        }
    )
}

private fun HTMLElement.setMediaElementStyle(corner: Dp, fit: MediaFit, alpha: Float = 1f, cssFilter: String = "", cssTransform: String = "") {
    val filterPart = if (cssFilter.isNotBlank()) "filter:$cssFilter;" else ""
    val transformPart = if (cssTransform.isNotBlank()) "transform:$cssTransform;" else ""
    setAttribute(
        "style",
        "width:100%; height:100%; display:block; object-fit:${fit.cssValue}; border-radius:${corner.value}px; background:transparent; opacity:$alpha; transition: opacity 80ms linear; pointer-events: none; $filterPart $transformPart"
    )
}

@Composable
fun AdaptivePair(
    modifier: Modifier = Modifier,
    spacing: Dp = 80.dp,
    first: @Composable (Modifier) -> Unit,
    second: @Composable (Modifier) -> Unit,
) {
    BoxWithConstraints(modifier = modifier.fillMaxWidth()) {
        if (maxWidth < 920.dp) {
            val isMobile = maxWidth < 600.dp
            Column(verticalArrangement = Arrangement.spacedBy(if (isMobile) 32.dp else 64.dp)) {
                first(Modifier.fillMaxWidth())
                second(Modifier.fillMaxWidth())
            }
        } else {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(spacing),
                verticalAlignment = Alignment.CenterVertically
            ) {
                first(Modifier.weight(1f))
                second(Modifier.weight(1f))
            }
        }
    }
}

/** Scrolling page column used by the Download and Together pages; keeps clear of the floating chrome. */
@Composable
fun DesktopScrollableColumn(
    modifier: Modifier = Modifier,
    columnModifier: Modifier = Modifier,
    scrollState: ScrollState = rememberScrollState(),
    verticalArrangement: Arrangement.Vertical = Arrangement.Top,
    horizontalAlignment: Alignment.Horizontal = Alignment.Start,
    content: @Composable ColumnScope.() -> Unit
) {
    val insets = LocalChromeInsets.current
    BoxWithConstraints(modifier = modifier) {
        val isMobile = maxWidth < 600.dp
        Box(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(scrollState)
                    .padding(top = insets.top, bottom = insets.bottom)
                    .then(columnModifier),
                verticalArrangement = verticalArrangement,
                horizontalAlignment = horizontalAlignment,
                content = content
            )
            if (!isMobile) {
                VerticalScrollbar(
                    modifier = Modifier.align(Alignment.CenterEnd).fillMaxHeight().padding(top = insets.top, bottom = 4.dp, end = 2.dp),
                    adapter = rememberScrollbarAdapter(scrollState = scrollState)
                )
            }
        }
    }
}


private val svgPainterCache = mutableMapOf<String, androidx.compose.ui.graphics.painter.Painter>()

/** Decodes (once, then cached) an SVG from composeResources/files into a painter; null while loading. */
@Composable
fun rememberSvgPainter(path: String): androidx.compose.ui.graphics.painter.Painter? {
    val density = androidx.compose.ui.platform.LocalDensity.current
    val painter by androidx.compose.runtime.produceState(svgPainterCache[path], path) {
        if (value == null) {
            value = runCatching {
                kittytunewebsite.generated.resources.Res.readBytes(path).decodeToSvgPainter(density)
            }.getOrNull()?.also { svgPainterCache[path] = it }
        }
    }
    return painter
}

/** An SVG from composeResources/files, rendered by Compose. */
@Composable
fun SvgImage(path: String, description: String?, modifier: Modifier = Modifier) {
    val density = androidx.compose.ui.platform.LocalDensity.current
    val painter by androidx.compose.runtime.produceState(svgPainterCache[path], path) {
        if (value == null) {
            value = runCatching {
                kittytunewebsite.generated.resources.Res.readBytes(path).let { bytes ->
                    bytes.decodeToSvgPainter(density)
                }
            }.getOrNull()?.also { svgPainterCache[path] = it }
        }
    }
    val p = painter
    if (p == null) Spacer(modifier) else androidx.compose.foundation.Image(p, description, modifier, contentScale = androidx.compose.ui.layout.ContentScale.Fit)
}
