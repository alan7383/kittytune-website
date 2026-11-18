@file:OptIn(kotlin.js.ExperimentalWasmJsInterop::class)
package com.alananasss.kittytunewebsite.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.EnterExitState
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.alananasss.kittytunewebsite.ui.theme.KittyTuneWebTheme
import com.materialkolor.PaletteStyle

enum class Page {
    HOME, FEATURES, TOGETHER, DOWNLOAD
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

private fun basePath(): String =
    kotlinx.browser.window.location.pathname.substringBeforeLast("/", "").removeSuffix("/")

private fun restoreRedirectIfPresent() {
    val savedPath = kotlinx.browser.window.sessionStorage.getItem("kittytune_redirect")
    if (savedPath != null) {
        kotlinx.browser.window.sessionStorage.removeItem("kittytune_redirect")
        kotlinx.browser.window.history.replaceState(null, "", savedPath)
    }
}

private fun getTogetherCodeFromUrl(): String? {
    restoreRedirectIfPresent()
    val full = kotlinx.browser.window.location.pathname + kotlinx.browser.window.location.search + kotlinx.browser.window.location.hash
    return TogetherWebLinks.extractCodeFromUrl(full)
}

private fun getPageFromUrl(): Page {
    restoreRedirectIfPresent()
    if (getTogetherCodeFromUrl() != null) return Page.TOGETHER
    val path = kotlinx.browser.window.location.pathname.removePrefix(basePath()).trim('/').lowercase()
    return when {
        path == "download" -> Page.DOWNLOAD
        path == "features" -> Page.FEATURES
        path == "together" || path == "listen" || path.startsWith("together/") || path.startsWith("listen/") -> Page.TOGETHER
        else -> Page.HOME
    }
}

private fun updateUrlForPage(page: Page, togetherCode: String?) {
    val base = basePath()
    val targetUrl = when (page) {
        Page.HOME -> "$base/"
        Page.FEATURES -> "$base/features"
        Page.TOGETHER -> if (!togetherCode.isNullOrBlank()) {
            "$base/?together=${TogetherWebLinks.displayCode(togetherCode)}"
        } else {
            "$base/together"
        }
        Page.DOWNLOAD -> "$base/download"
    }
    val currentUrl = kotlinx.browser.window.location.pathname + kotlinx.browser.window.location.search
    if (currentUrl != targetUrl) {
        kotlinx.browser.window.history.pushState(null, "", targetUrl)
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun App() {
    var currentLocale by remember { mutableStateOf(detectInitialWebLocale()) }
    var activeTogetherCode by remember { mutableStateOf(getTogetherCodeFromUrl()) }
    var currentPage by remember { mutableStateOf(getPageFromUrl()) }
    var selectedColor by remember {
        mutableStateOf(
            kotlinx.browser.localStorage.getItem("theme_color")?.let { savedName ->
                colorPresets.find { it.name == savedName }
            } ?: colorPresets.find { it.name == "Indigo" } ?: colorPresets[0]
        )
    }
    var selectedStyle by remember {
        mutableStateOf(
            kotlinx.browser.localStorage.getItem("theme_style")?.let { savedName ->
                paletteStyles.find { it.first == savedName }
            } ?: paletteStyles.find { it.first == "Expressive" } ?: paletteStyles[0]
        )
    }
    var showThemePanel by remember { mutableStateOf(false) }
    var showLanguagePanel by remember { mutableStateOf(false) }
    var isAppLoaded by remember { mutableStateOf(activeTogetherCode != null) }

    LaunchedEffect(currentLocale) {
        persistWebLocale(currentLocale)
        kotlinx.browser.document.documentElement?.setAttribute("lang", currentLocale.code)
    }
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

        if (activeTogetherCode == null) {
            kotlinx.coroutines.delay(1200L)
        }
        isAppLoaded = true
    }

    LaunchedEffect(currentPage, activeTogetherCode) {
        updateUrlForPage(currentPage, activeTogetherCode)
    }

    DisposableEffect(Unit) {
        val listener: (org.w3c.dom.events.Event) -> Unit = {
            activeTogetherCode = getTogetherCodeFromUrl()
            currentPage = getPageFromUrl()
        }
        kotlinx.browser.window.addEventListener("popstate", listener)
        kotlinx.browser.window.addEventListener("hashchange", listener)
        onDispose {
            kotlinx.browser.window.removeEventListener("popstate", listener)
            kotlinx.browser.window.removeEventListener("hashchange", listener)
        }
    }

    LaunchedEffect(showThemePanel, showLanguagePanel, isAppLoaded) {
        val hideMedia = showThemePanel || showLanguagePanel || !isAppLoaded

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

    val strings = appChromeStrings(currentLocale)

    CompositionLocalProvider(LocalWebLocale provides currentLocale) {
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
                                    icon = { Icon(Icons.Filled.Home, contentDescription = strings.navHome) },
                                    label = { Text(strings.navHome, maxLines = 1, overflow = TextOverflow.Ellipsis) }
                                )
                                NavigationBarItem(
                                    selected = currentPage == Page.FEATURES,
                                    onClick = { currentPage = Page.FEATURES },
                                    icon = { Icon(Icons.Filled.Star, contentDescription = strings.navFeatures) },
                                    label = { Text(strings.navFeatures, maxLines = 1, overflow = TextOverflow.Ellipsis) }
                                )
                                NavigationBarItem(
                                    selected = currentPage == Page.TOGETHER,
                                    onClick = { currentPage = Page.TOGETHER },
                                    icon = { Icon(Icons.Filled.Headphones, contentDescription = strings.navTogether) },
                                    label = { Text(strings.navTogether, maxLines = 1, overflow = TextOverflow.Ellipsis) }
                                )
                                NavigationBarItem(
                                    selected = currentPage == Page.DOWNLOAD,
                                    onClick = { currentPage = Page.DOWNLOAD },
                                    icon = { Icon(Icons.Filled.Download, contentDescription = strings.navDownload) },
                                    label = { Text(strings.navDownload, maxLines = 1, overflow = TextOverflow.Ellipsis) }
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
                                        NavBarItem(strings.navHome, currentPage == Page.HOME) { currentPage = Page.HOME }
                                        NavBarItem(strings.navFeatures, currentPage == Page.FEATURES) { currentPage = Page.FEATURES }
                                        NavBarItem(strings.navTogether, currentPage == Page.TOGETHER) { currentPage = Page.TOGETHER }
                                        NavBarItem(strings.navDownload, currentPage == Page.DOWNLOAD) { currentPage = Page.DOWNLOAD }

                                        Spacer(modifier = Modifier.width(4.dp))
                                    }

                                    LanguageSwitcherButton(
                                        currentLocale = currentLocale,
                                        isOpen = showLanguagePanel,
                                        contentDescription = strings.changeLanguageA11y,
                                        onClick = {
                                            showLanguagePanel = !showLanguagePanel
                                            if (showLanguagePanel) showThemePanel = false
                                        }
                                    )

                                    IconButton(
                                        onClick = {
                                            showThemePanel = !showThemePanel
                                            if (showThemePanel) showLanguagePanel = false
                                        }
                                    ) {
                                        Icon(
                                            if (showThemePanel) Icons.Filled.Close else Icons.Filled.Palette,
                                            contentDescription = if (showThemePanel) strings.closeA11y else strings.customizeThemeA11y,
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
                                    Page.TOGETHER -> ListenAlongPage(
                                        activeCode = activeTogetherCode,
                                        onCodeSelected = { code ->
                                            activeTogetherCode = code
                                            currentPage = Page.TOGETHER
                                        },
                                        onDownloadClick = { currentPage = Page.DOWNLOAD },
                                    )
                                    Page.DOWNLOAD -> DownloadPage()
                                }
                            }
                        }
                    }
                }

                if (showLanguagePanel) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .zIndex(150f)
                            .clickable { showLanguagePanel = false }
                    )
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(top = 72.dp, end = 16.dp)
                            .zIndex(200f)
                            .clip(RoundedCornerShape(28.dp))
                            .background(MaterialTheme.colorScheme.surfaceContainer)
                            .clickable {}
                            .padding(20.dp)
                            .width(296.dp)
                    ) {
                        LanguagePanel(
                            currentLocale = currentLocale,
                            title = strings.languagePanelTitle,
                            closeLabel = strings.closeA11y,
                            onSelectLocale = { locale ->
                                currentLocale = locale
                                showLanguagePanel = false
                            },
                            onClose = { showLanguagePanel = false }
                        )
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
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun LanguageSwitcherButton(
    currentLocale: WebLocale,
    isOpen: Boolean,
    contentDescription: String,
    onClick: () -> Unit,
) {
    val containerColor by animateColorAsState(
        targetValue = if (isOpen) MaterialTheme.colorScheme.primaryContainer
        else MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.72f),
        animationSpec = tween(durationMillis = 220)
    )
    val contentColor by animateColorAsState(
        targetValue = if (isOpen) MaterialTheme.colorScheme.onPrimaryContainer
        else MaterialTheme.colorScheme.onSecondaryContainer,
        animationSpec = tween(durationMillis = 220)
    )

    Button(
        onClick = onClick,
        shapes = ButtonDefaults.shapes(shape = CircleShape),
        colors = ButtonDefaults.buttonColors(
            containerColor = containerColor,
            contentColor = contentColor
        ),
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
        elevation = null,
        modifier = Modifier.height(38.dp)
    ) {
        Icon(
            Icons.Filled.Language,
            contentDescription = contentDescription,
            modifier = Modifier.size(18.dp)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = currentLocale.shortLabel,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.ExtraBold
        )
        Spacer(modifier = Modifier.width(2.dp))
        Icon(
            if (isOpen) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
            contentDescription = null,
            modifier = Modifier.size(16.dp)
        )
    }
}

@Composable
private fun LanguagePanel(
    currentLocale: WebLocale,
    title: String,
    closeLabel: String,
    onSelectLocale: (WebLocale) -> Unit,
    onClose: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Filled.Language,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
                Text(
                    title,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            IconButton(
                onClick = onClose,
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    Icons.Filled.Close,
                    contentDescription = closeLabel,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(18.dp)
                )
            }
        }

        Column(
            verticalArrangement = Arrangement.spacedBy(4.dp),
            modifier = Modifier
                .verticalScroll(rememberScrollState())
                .heightIn(max = 420.dp)
        ) {
            WebLocale.entries.forEach { locale ->
                val isSelected = locale == currentLocale
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(
                            if (isSelected) MaterialTheme.colorScheme.primaryContainer
                            else Color.Transparent
                        )
                        .clickable { onSelectLocale(locale) }
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = if (isSelected) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.surfaceContainerHighest
                        ) {
                            Text(
                                text = locale.shortLabel,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.ExtraBold,
                                color = if (isSelected) MaterialTheme.colorScheme.onPrimary
                                else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                        Text(
                            text = locale.nativeName,
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer
                            else MaterialTheme.colorScheme.onSurface
                        )
                    }
                    if (isSelected) {
                        Icon(
                            Icons.Filled.Check,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.size(18.dp)
                        )
                    }
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
    val strings = appChromeStrings(LocalWebLocale.current)

    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Filled.Palette, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
            Text(
                strings.customizeTitle,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                strings.seedColorLabel,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                strings.colorName(selectedColor.name),
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
        }

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
                                contentDescription = strings.colorName(preset.name),
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }
        }

        Text(
            strings.paletteStyleLabel,
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
                        strings.paletteStyleName(style.first),
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
