package com.alananasss.kittytunewebsite.ui

import androidx.compose.foundation.VerticalScrollbar
import androidx.compose.foundation.rememberScrollbarAdapter
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.HtmlElementView
import kotlinx.browser.document
import org.w3c.dom.HTMLElement
import org.w3c.dom.HTMLImageElement
import org.w3c.dom.HTMLVideoElement

val LocalPageAlpha = compositionLocalOf { 1f }

data class MediaAsset(
    val fileName: String,
    val title: String,
    val caption: String,
    val fit: MediaFit = MediaFit.Cover,
    val linkUrl: String? = null,
    val linkText: String? = null
) {
    val isVideo: Boolean get() = fileName.endsWith(".mp4", ignoreCase = true)
    val url: String get() = "media/${fileName.replace(" ", "%20")}"
}

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
    BoxWithConstraints(modifier = modifier) {
        val isMobile = maxWidth < 600.dp
        Column(
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = if (centered) Alignment.CenterHorizontally else Alignment.Start
        ) {
            Text(
                text = title,
                style = if (isMobile) MaterialTheme.typography.headlineLarge else MaterialTheme.typography.displayMedium,
                fontWeight = FontWeight.Black,
                color = MaterialTheme.colorScheme.onSurface,
                lineHeight = if (isMobile) 40.sp else 52.sp,
                textAlign = if (centered) TextAlign.Center else TextAlign.Start
            )
            Text(
                text = description,
                style = if (isMobile) MaterialTheme.typography.bodyLarge else MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                lineHeight = if (isMobile) 28.sp else 32.sp,
                textAlign = if (centered) TextAlign.Center else TextAlign.Start
            )
        }
    }
}

@Composable
fun MediaCard(
    asset: MediaAsset,
    modifier: Modifier = Modifier,
    aspectRatio: Float = 16f / 10f,
    corner: Dp = 24.dp,
    containerColor: Color = MaterialTheme.colorScheme.surfaceContainerHigh,
    innerPadding: Dp = 0.dp,
    imageCssFilter: String = ""
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(aspectRatio),
            shape = RoundedCornerShape(corner),
            color = containerColor,
        ) {
            Box(
                modifier = Modifier.fillMaxSize().padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                val innerCorner = if (innerPadding > 0.dp) 12.dp else corner
                
                if (asset.isVideo) {
                    HtmlVideo(
                        src = asset.url,
                        corner = innerCorner,
                        fit = asset.fit,
                        modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(innerCorner))
                    )
                } else {
                    HtmlImage(
                        src = asset.url,
                        description = asset.title,
                        corner = innerCorner,
                        fit = asset.fit,
                        cssFilter = imageCssFilter,
                        modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(innerCorner))
                    )
                }
            }
        }

        if (asset.title.isNotBlank() || asset.caption.isNotBlank() || asset.linkText != null) {
            Column(
                modifier = Modifier.padding(horizontal = 8.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                if (asset.title.isNotBlank()) {
                    Text(
                        text = asset.title,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                if (asset.caption.isNotBlank()) {
                    Text(
                        text = asset.caption,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        lineHeight = 24.sp
                    )
                }
                if (asset.linkUrl != null && asset.linkText != null) {
                    Text(
                        text = asset.linkText,
                        style = MaterialTheme.typography.labelLarge.copy(
                            fontWeight = FontWeight.Bold
                        ),
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .clickable { kotlinx.browser.window.open(asset.linkUrl, "_blank") }
                            .padding(vertical = 4.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun FeatureIsland(
    title: String,
    description: String,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(48.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow
    ) {
        BoxWithConstraints {
            val isCompact = maxWidth < 800.dp
            val isMobile = maxWidth < 600.dp
            val padding = if (isMobile) 24.dp else if (isCompact) 32.dp else 64.dp
            
            Column(
                modifier = Modifier.padding(padding),
                verticalArrangement = Arrangement.spacedBy(if (isMobile) 32.dp else 56.dp)
            ) {
                SectionHeader(title, description, Modifier.fillMaxWidth(if (isCompact) 1f else 0.75f))
                content()
            }
        }
    }
}

@Composable
fun ComparisonBoard(
    assetLeft: MediaAsset,
    assetRight: MediaAsset,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(32.dp),
        color = MaterialTheme.colorScheme.surfaceContainer
    ) {
        AdaptivePair(
            spacing = 24.dp,
            modifier = Modifier.padding(24.dp),
            first = { childMod ->
                MediaCard(asset = assetLeft, aspectRatio = 16f/10f, corner = 20.dp, modifier = childMod, containerColor = Color.Black)
            },
            second = { childMod ->
                MediaCard(asset = assetRight, aspectRatio = 16f/10f, corner = 20.dp, modifier = childMod, containerColor = MaterialTheme.colorScheme.surfaceContainerHigh)
            }
        )
    }
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun HtmlVideo(src: String, corner: Dp, fit: MediaFit, modifier: Modifier = Modifier) {
    val pageAlpha = LocalPageAlpha.current
    HtmlElementView(
        factory = {
            (document.createElement("video") as HTMLVideoElement).apply {
                setAttribute("src", src)
                this.autoplay = true
                this.loop = true
                this.muted = true
                this.defaultMuted = true
                setAttribute("playsinline", "")
                setAttribute("preload", "metadata")
                setMediaElementStyle(corner, fit, pageAlpha)
            }
        },
        modifier = modifier,
        update = { element ->
            (element.parentElement as? HTMLElement)?.style?.setProperty("pointer-events", "none")
            if (element.getAttribute("src") != src) {
                element.setAttribute("src", src)
                element.load()
            }
            element.muted = true
            element.setMediaElementStyle(corner, fit, pageAlpha)
        }
    )
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun HtmlImage(src: String, description: String, corner: Dp, fit: MediaFit, modifier: Modifier = Modifier, cssFilter: String = "", cssTransform: String = "") {
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

@Composable
fun AdaptiveGrid(
    items: List<MediaAsset>,
    modifier: Modifier = Modifier,
    aspectRatio: Float = 16f / 10f,
    innerPadding: Dp = 0.dp,
    imageCssFilter: String = ""
) {
    BoxWithConstraints(modifier = modifier.fillMaxWidth()) {
        if (maxWidth < 860.dp) {
            val isMobile = maxWidth < 600.dp
            Column(verticalArrangement = Arrangement.spacedBy(if (isMobile) 24.dp else 48.dp)) {
                items.forEach { asset ->
                    MediaCard(
                        asset = asset, 
                        aspectRatio = aspectRatio, 
                        innerPadding = innerPadding, 
                        imageCssFilter = imageCssFilter, 
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        } else {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(32.dp)
            ) {
                items.forEach { asset ->
                    MediaCard(
                        asset = asset, 
                        aspectRatio = aspectRatio, 
                        innerPadding = innerPadding, 
                        imageCssFilter = imageCssFilter, 
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

@Composable
fun DesktopScrollableColumn(
    modifier: Modifier = Modifier,
    columnModifier: Modifier = Modifier,
    scrollState: ScrollState = rememberScrollState(),
    verticalArrangement: Arrangement.Vertical = Arrangement.Top,
    horizontalAlignment: Alignment.Horizontal = Alignment.Start,
    content: @Composable ColumnScope.() -> Unit
) {
    BoxWithConstraints(modifier = modifier) {
        val isMobile = maxWidth < 600.dp
        Box(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(scrollState)
                    .then(columnModifier),
                verticalArrangement = verticalArrangement,
                horizontalAlignment = horizontalAlignment,
                content = content
            )
            if (!isMobile) {
                VerticalScrollbar(
                    modifier = Modifier.align(Alignment.CenterEnd).fillMaxHeight().padding(vertical = 4.dp, horizontal = 2.dp),
                    adapter = rememberScrollbarAdapter(scrollState = scrollState)
                )
            }
        }
    }
}
