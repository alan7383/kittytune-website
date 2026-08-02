@file:OptIn(kotlin.js.ExperimentalWasmJsInterop::class)
package com.alananasss.kittytunewebsite.ui

import androidx.compose.animation.EnterExitState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.alananasss.kittytunewebsite.ui.theme.KittyTuneWebTheme
import com.materialkolor.PaletteStyle

enum class Page {
    HOME, FEATURES, DOWNLOAD
}

data class ColorPreset(val name: String, val color: Color)

val colorPresets = listOf(
    ColorPreset("Blue", Color(0xFF1976D2)),
    ColorPreset("Purple", Color(0xFF7B1FA2)),
    ColorPreset("Teal", Color(0xFF00897B)),
    ColorPreset("Red", Color(0xFFC62828)),
    ColorPreset("Orange", Color(0xFFE65100)),
    ColorPreset("Pink", Color(0xFFAD1457)),
    ColorPreset("Indigo", Color(0xFF283593)),
    ColorPreset("Cyan", Color(0xFF00838F)),
    ColorPreset("Green", Color(0xFF2E7D32)),
    ColorPreset("Gold", Color(0xFFF9A825)),
)

val paletteStyles = listOf(
    "Vibrant" to PaletteStyle.Vibrant,
    "Tonal Spot" to PaletteStyle.TonalSpot,
    "Expressive" to PaletteStyle.Expressive,
    "Rainbow" to PaletteStyle.Rainbow,
    "Fruit Salad" to PaletteStyle.FruitSalad,
    "Fidelity" to PaletteStyle.Fidelity,
    "Content" to PaletteStyle.Content,
    "Monochrome" to PaletteStyle.Monochrome,
    "Neutral" to PaletteStyle.Neutral,
)

private fun getPageFromUrl(): Page {
    val path = kotlinx.browser.window.location.pathname.lowercase().trim('/')
    return when {
        path == "download" -> Page.DOWNLOAD
        path == "features" -> Page.FEATURES
        else -> Page.HOME
    }
}

private fun updateUrlForPage(page: Page) {
    val targetPath = when (page) {
        Page.HOME -> "/"
        Page.FEATURES -> "/features"
        Page.DOWNLOAD -> "/download"
    }
    val currentPath = kotlinx.browser.window.location.pathname
    if (currentPath != targetPath) {
        kotlinx.browser.window.history.pushState(null, "", targetPath)
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun App() {
    var currentPage by remember { mutableStateOf(getPageFromUrl()) }
    var selectedColor by remember { 
        mutableStateOf(
            kotlinx.browser.localStorage.getItem("theme_color")?.let { savedName ->
                colorPresets.find { it.name == savedName }
            } ?: colorPresets[0]
        ) 
    }
    var selectedStyle by remember { 
        mutableStateOf(
            kotlinx.browser.localStorage.getItem("theme_style")?.let { savedName ->
                paletteStyles.find { it.first == savedName }
            } ?: paletteStyles.find { it.first == "Monochrome" } ?: paletteStyles[0]
        ) 
    }
    var showThemePanel by remember { mutableStateOf(false) }
    var isAppLoaded by remember { mutableStateOf(false) }

    LaunchedEffect(selectedColor) {
        kotlinx.browser.localStorage.setItem("theme_color", selectedColor.name)
    }
    LaunchedEffect(selectedStyle) {
        kotlinx.browser.localStorage.setItem("theme_style", selectedStyle.first)
    }

    LaunchedEffect(Unit) {
        val style = kotlinx.browser.document.createElement("style")
        style.innerHTML = """
            body.hide-native-media video, body.hide-native-media img {
                opacity: 0 !important;
                visibility: hidden !important;
                pointer-events: none !important;
            }
        """.trimIndent()
        kotlinx.browser.document.head?.appendChild(style)

        kotlinx.coroutines.delay(1200)
        isAppLoaded = true
    }

    LaunchedEffect(currentPage) {
        updateUrlForPage(currentPage)
    }

    DisposableEffect(Unit) {
        val listener: (org.w3c.dom.events.Event) -> Unit = {
            currentPage = getPageFromUrl()
        }
        kotlinx.browser.window.addEventListener("popstate", listener)
        kotlinx.browser.window.addEventListener("hashchange", listener)
        onDispose {
            kotlinx.browser.window.removeEventListener("popstate", listener)
            kotlinx.browser.window.removeEventListener("hashchange", listener)
        }
    }

    LaunchedEffect(showThemePanel, isAppLoaded) {
        val hideMedia = showThemePanel || !isAppLoaded
        
        if (hideMedia) {
            kotlinx.browser.document.body?.classList?.add("hide-native-media")
        } else {
            kotlinx.browser.document.body?.classList?.remove("hide-native-media")
        }

        val elements = kotlinx.browser.document.querySelectorAll("video, img")
        for (i in 0 until elements.length) {
            val el = elements.item(i) as? org.w3c.dom.HTMLElement ?: continue
            var wrapper: org.w3c.dom.HTMLElement? = el
            while (wrapper != null && wrapper.tagName.lowercase() != "body") {
                val pos = kotlinx.browser.window.getComputedStyle(wrapper).getPropertyValue("position")
                if (pos == "absolute" || pos == "fixed") break
                wrapper = wrapper.parentElement as? org.w3c.dom.HTMLElement
            }
            val target = wrapper ?: el
            if (hideMedia) {
                target.style.setProperty("display", "none")
            } else {
                target.style.removeProperty("display")
            }
        }
    }


    KittyTuneWebTheme(
        seedColor = selectedColor.color,
        paletteStyle = selectedStyle.second
    ) {
        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            val isMobile = maxWidth < 600.dp

            AnimatedVisibility(
                visible = !isAppLoaded,
                exit = fadeOut(animationSpec = tween(600)),
                modifier = Modifier.zIndex(300f)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.background),
                    contentAlignment = Alignment.Center
                ) {
                    LoadingScreen()
                }
            }

            Scaffold(
                bottomBar = {
                    if (isMobile) {
                        NavigationBar {
                            NavigationBarItem(
                                selected = currentPage == Page.HOME,
                                onClick = { currentPage = Page.HOME },
                                icon = { Icon(Icons.Filled.Home, contentDescription = "Home") },
                                label = { Text("Home") }
                            )
                            NavigationBarItem(
                                selected = currentPage == Page.FEATURES,
                                onClick = { currentPage = Page.FEATURES },
                                icon = { Icon(Icons.Filled.Star, contentDescription = "Features") },
                                label = { Text("Features") }
                            )
                            NavigationBarItem(
                                selected = currentPage == Page.DOWNLOAD,
                                onClick = { currentPage = Page.DOWNLOAD },
                                icon = { Icon(Icons.Filled.Download, contentDescription = "Download") },
                                label = { Text("Download") }
                            )
                        }
                    }
                },
                topBar = {
                    TopAppBar(
                        title = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    Icons.Filled.MusicNote,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    "KittyTune",
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        },
                        actions = {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier
                                    .padding(end = 16.dp)
                                    .horizontalScroll(rememberScrollState()),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                if (!isMobile) {
                                    NavBarItem("Home", currentPage == Page.HOME) { currentPage = Page.HOME }
                                    NavBarItem("Features", currentPage == Page.FEATURES) { currentPage = Page.FEATURES }
                                    NavBarItem("Download", currentPage == Page.DOWNLOAD) { currentPage = Page.DOWNLOAD }

                                    Spacer(modifier = Modifier.width(8.dp))
                                }

                                IconButton(onClick = { showThemePanel = !showThemePanel }) {
                                    Icon(
                                        if (showThemePanel) Icons.Filled.Close else Icons.Filled.Palette,
                                        contentDescription = "Customize theme",
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        },
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f)
                        )
                    )
                },
                containerColor = MaterialTheme.colorScheme.background
            ) { paddingValues ->
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                ) {
                    AnimatedContent(
                        targetState = currentPage,
                        transitionSpec = {
                            fadeIn(animationSpec = tween(400)) togetherWith fadeOut(animationSpec = tween(400))
                        },
                        label = "page_transition"
                    ) { page ->
                        val pageAlpha by transition.animateFloat(
                            transitionSpec = { tween(400) },
                            label = "page_alpha"
                        ) { state ->
                            if (state == EnterExitState.Visible) 1f else 0f
                        }

                        CompositionLocalProvider(LocalPageAlpha provides pageAlpha) {
                            when (page) {
                                Page.HOME -> HomePage { currentPage = Page.DOWNLOAD }
                                Page.FEATURES -> FeaturesPage()
                                Page.DOWNLOAD -> DownloadPage()
                            }
                        }
                    }
                }
            }

            if (showThemePanel) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .zIndex(150f)
                        .clickable { showThemePanel = false }
                )
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(top = 72.dp, end = 16.dp)
                        .zIndex(200f)
                        .clip(RoundedCornerShape(24.dp))
                        .background(MaterialTheme.colorScheme.surfaceContainer)
                        .clickable {}
                        .padding(24.dp)
                        .width(320.dp)
                ) {
                    ThemePanel(
                        selectedColor = selectedColor,
                        onColorSelect = { selectedColor = it },
                        selectedStyle = selectedStyle,
                        onStyleSelect = { selectedStyle = it }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun ThemePanel(
    selectedColor: ColorPreset,
    onColorSelect: (ColorPreset) -> Unit,
    selectedStyle: Pair<String, PaletteStyle>,
    onStyleSelect: (Pair<String, PaletteStyle>) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Filled.Palette, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
            Text(
                "Customize",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
        }

        Text(
            "Seed color",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        val chunked = colorPresets.chunked(5)
        chunked.forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                row.forEach { preset ->
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(preset.color)
                            .clickable { onColorSelect(preset) },
                        contentAlignment = Alignment.Center
                    ) {
                        if (selectedColor.name == preset.name) {
                            Icon(
                                Icons.Filled.Check,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }
        }

        Text(
            "Palette style",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Column(
            verticalArrangement = Arrangement.spacedBy(4.dp),
            modifier = Modifier.verticalScroll(rememberScrollState()).heightIn(max = 400.dp)
        ) {
            paletteStyles.forEach { style ->
                val isSelected = selectedStyle.first == style.first
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(
                            if (isSelected) MaterialTheme.colorScheme.primaryContainer
                            else Color.Transparent
                        )
                        .clickable { onStyleSelect(style) }
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        style.first,
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer
                        else MaterialTheme.colorScheme.onSurface
                    )
                    if (isSelected) {
                        Icon(
                            Icons.Filled.Check,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun NavBarItem(text: String, isSelected: Boolean, onClick: () -> Unit) {
    val containerColor by animateColorAsState(
        targetValue = if (isSelected) MaterialTheme.colorScheme.secondaryContainer else Color.Transparent,
        animationSpec = tween(durationMillis = 250)
    )
    val contentColor by animateColorAsState(
        targetValue = if (isSelected) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.onSurface,
        animationSpec = tween(durationMillis = 250)
    )

    Button(
        onClick = onClick,
        shapes = ButtonDefaults.shapes(),
        colors = ButtonDefaults.buttonColors(
            containerColor = containerColor,
            contentColor = contentColor
        ),
        elevation = null
    ) {
        Text(
            text = text,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
            fontSize = 14.sp
        )
    }
}
