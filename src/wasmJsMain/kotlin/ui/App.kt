@file:OptIn(kotlin.js.ExperimentalWasmJsInterop::class, ExperimentalMaterial3ExpressiveApi::class)

package com.alananasss.kittytunewebsite.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.EnterExitState
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.geometry.isSpecified
import kotlinx.coroutines.launch
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInParent
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.alananasss.kittytunewebsite.ui.kit.ChromeInsets
import com.alananasss.kittytunewebsite.ui.kit.KittyLogo
import com.alananasss.kittytunewebsite.ui.kit.LocalChromeInsets
import com.alananasss.kittytunewebsite.ui.kit.MorphingBlob
import com.alananasss.kittytunewebsite.ui.kit.PolygonShape
import com.alananasss.kittytunewebsite.ui.kit.springyPress
import com.alananasss.kittytunewebsite.ui.theme.KittyTuneWebTheme

enum class Page { HOME, FEATURES, TOGETHER, DOWNLOAD }

private const val KEY_COLOR = "kt_theme_color_v3"
private const val KEY_STYLE = "kt_theme_style_v3"
private const val KEY_DARK = "kt_theme_dark_v3"

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

private fun storageGet(key: String): String? = runCatching { kotlinx.browser.localStorage.getItem(key) }.getOrNull()
private fun storageSet(key: String, value: String) { runCatching { kotlinx.browser.localStorage.setItem(key, value) } }

private fun systemPrefersDark(): Boolean = js("(function(){ try { return !window.matchMedia || window.matchMedia('(prefers-color-scheme: dark)').matches; } catch (e) { return true; } })()")

private fun hideHtmlSplash() {
    js("(function(){ try { var s = document.getElementById('kt-splash'); if (s) { s.classList.add('gone'); setTimeout(function(){ try { if (window.__ktLoaderWorker) window.__ktLoaderWorker.terminate(); } catch (e) {} if (s.parentNode) s.parentNode.removeChild(s); }, 600); } } catch (e) {} })()")
}

private class ThemeReveal {
    var capturing by mutableStateOf(false)
    var snapshot by mutableStateOf<androidx.compose.ui.graphics.ImageBitmap?>(null)
    val progress = androidx.compose.animation.core.Animatable(1f)
    var origin = androidx.compose.ui.geometry.Offset.Unspecified
    var busy = false
}

/** Navigation callbacks for pages that link elsewhere on the site. */
val LocalNavigate = staticCompositionLocalOf<(Page) -> Unit> { {} }

private data class NavItem(val page: Page, val label: String, val icon: ImageVector)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun App() {
    var currentLocale by remember { mutableStateOf(detectInitialWebLocale()) }
    var activeTogetherCode by remember { mutableStateOf(getTogetherCodeFromUrl()) }
    var currentPage by remember { mutableStateOf(getPageFromUrl()) }
    var selectedColor by remember {
        mutableStateOf(storageGet(KEY_COLOR)?.let { n -> colorPresets.find { it.name == n } } ?: colorPresets.first())
    }
    var selectedStyle by remember {
        mutableStateOf(storageGet(KEY_STYLE)?.let { n -> paletteStyles.find { it.first == n } } ?: paletteStyles.first())
    }
    var isDark by remember { mutableStateOf(storageGet(KEY_DARK)?.let { it == "1" } ?: runCatching { systemPrefersDark() }.getOrDefault(true)) }
    var showThemePanel by remember { mutableStateOf(false) }
    var showLanguagePanel by remember { mutableStateOf(false) }
    var isAppLoaded by remember { mutableStateOf(false) }

    LaunchedEffect(currentLocale) {
        persistWebLocale(currentLocale)
        kotlinx.browser.document.documentElement?.setAttribute("lang", currentLocale.code)
    }
    LaunchedEffect(selectedColor) { storageSet(KEY_COLOR, selectedColor.name) }
    LaunchedEffect(selectedStyle) { storageSet(KEY_STYLE, selectedStyle.first) }
    LaunchedEffect(isDark) {
        storageSet(KEY_DARK, if (isDark) "1" else "0")
        runCatching { kotlinx.browser.document.body?.style?.setProperty("background-color", if (isDark) "#0c0e14" else "#f8f9ff") }
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
    }

    LaunchedEffect(currentPage, activeTogetherCode) {
        updateUrlForPage(currentPage, activeTogetherCode)
        runCatching {
            kotlinx.browser.document.title = when (currentPage) {
                Page.HOME -> "KittyTune · Open-source music player"
                Page.FEATURES -> "Features · KittyTune"
                Page.TOGETHER -> "Together · KittyTune"
                Page.DOWNLOAD -> "Download · KittyTune"
            }
        }
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

    // HTML <img>/<video> elements float above the canvas; hide them while overlays are open.
    LaunchedEffect(showThemePanel, showLanguagePanel, isAppLoaded) {
        val hideMedia = showThemePanel || showLanguagePanel || !isAppLoaded
        if (hideMedia) kotlinx.browser.document.body?.classList?.add("hide-native-media")
        else kotlinx.browser.document.body?.classList?.remove("hide-native-media")
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
            if (hideMedia) target.style.setProperty("display", "none") else target.style.removeProperty("display")
        }
    }

    val strings = appChromeStrings(currentLocale)
    val site = siteStrings(currentLocale)
    val navigate: (Page) -> Unit = { p ->
        currentPage = p
        showThemePanel = false
        showLanguagePanel = false
    }
    // Theme, colour and language switches recompose the whole site once. Instead of animating ~35
    // colours (which recomposed everything on every frame for half a second), the current frame is
    // frozen into a bitmap, the change is applied underneath in one go, and the old frame is wiped
    // away by a circle growing from where you clicked — a draw-only animation, no recomposition.
    val reveal = remember { ThemeReveal() }
    val rootLayer = androidx.compose.ui.graphics.rememberGraphicsLayer()
    val revealScope = rememberCoroutineScope()
    fun withReveal(apply: () -> Unit) {
        if (reveal.busy) { apply(); return }
        reveal.busy = true
        revealScope.launch {
            try {
                // One frame records the page into the layer, then everything starts at once: the
                // circle begins growing immediately (over identical pixels until the new theme lands),
                // so there is no visible pause while the site recomposes underneath.
                reveal.capturing = true
                androidx.compose.runtime.withFrameNanos { }
                val bmp = runCatching { rootLayer.toImageBitmap() }.getOrNull()
                reveal.capturing = false
                if (bmp == null) { apply(); return@launch }
                reveal.progress.snapTo(0f)
                reveal.snapshot = bmp
                apply()
                reveal.progress.animateTo(1f, tween(520, easing = androidx.compose.animation.core.CubicBezierEasing(0.3f, 0f, 0.1f, 1f)))
            } finally {
                reveal.snapshot = null
                reveal.capturing = false
                reveal.busy = false
            }
        }
    }
    val controller = ThemeController(
        selectedColor = selectedColor,
        selectColor = { c -> if (c != selectedColor) withReveal { selectedColor = c } },
        selectedStyle = selectedStyle,
        selectStyle = { st -> if (st != selectedStyle) withReveal { selectedStyle = st } },
        isDark = isDark,
        setDark = { d -> if (d != isDark) withReveal { isDark = d } },
    )

    CompositionLocalProvider(
        LocalWebLocale provides currentLocale,
        LocalThemeController provides controller,
        LocalNavigate provides navigate,
    ) {
        KittyTuneWebTheme(seedColor = selectedColor.color, paletteStyle = selectedStyle.second, isDark = isDark) {
            val cs = MaterialTheme.colorScheme
            // The HTML splash (the same M3 LoadingIndicator, drawn before the app downloads) stays up
            // until the first frames have settled: fonts, first composition and image decodes all
            // happen behind it, so the page appears already smooth instead of stuttering in.
            LaunchedEffect(Unit) {
                var last = androidx.compose.runtime.withFrameNanos { it }
                val start = last
                var calm = 0
                val budget = if (activeTogetherCode == null) 2_500_000_000L else 600_000_000L
                while (calm < 8 && last - start < budget) {
                    androidx.compose.runtime.withFrameNanos { now ->
                        if (now - last < 22_000_000L) calm++ else calm = 0
                        last = now
                    }
                }
                isAppLoaded = true
                hideHtmlSplash()
            }
            // Pre-build every preset's scheme (both modes) while idle, so picking a colour or toggling
            // dark mode later is pure recomposition, with no colour maths on the click.
            LaunchedEffect(isAppLoaded, selectedStyle) {
                if (!isAppLoaded) return@LaunchedEffect
                kotlinx.coroutines.delay(1500)
                for (dark in listOf(isDark, !isDark)) for (p in colorPresets) {
                    com.alananasss.kittytunewebsite.ui.theme.kittyScheme(p.color, dark, selectedStyle.second)
                    kotlinx.coroutines.delay(80)
                }
            }
            // Remember the theme colours so the next visit's splash loader matches them exactly.
            LaunchedEffect(cs.primary, cs.surface) {
                fun hex(c: Color): String {
                    val v = c.toArgb() and 0xFFFFFF
                    return "#" + v.toString(16).padStart(6, '0')
                }
                storageSet("kt_splash_colors", hex(cs.primary) + "|" + hex(cs.surface))
            }
            BoxWithConstraints(
                Modifier.fillMaxSize()
                    .pointerInput(Unit) {
                        awaitPointerEventScope {
                            while (true) {
                                val e = awaitPointerEvent(androidx.compose.ui.input.pointer.PointerEventPass.Initial)
                                if (e.type == androidx.compose.ui.input.pointer.PointerEventType.Press) {
                                    e.changes.firstOrNull()?.let { reveal.origin = it.position }
                                }
                            }
                        }
                    }
                    .drawWithContent {
                        if (reveal.capturing) {
                            rootLayer.record { this@drawWithContent.drawContent() }
                            drawLayer(rootLayer)
                        } else drawContent()
                    }
                    .background(cs.surface)
            ) {
                val compact = maxWidth < 860.dp
                val navItems = listOf(
                    NavItem(Page.HOME, strings.navHome, Icons.Rounded.Home),
                    NavItem(Page.FEATURES, strings.navFeatures, Icons.Rounded.AutoAwesome),
                    NavItem(Page.TOGETHER, strings.navTogether, Icons.Rounded.Headphones),
                    NavItem(Page.DOWNLOAD, strings.navDownload, Icons.Rounded.Download),
                )
                val insets = if (compact) ChromeInsets(top = 72.dp, bottom = 96.dp) else ChromeInsets(top = 88.dp, bottom = 0.dp)

                CompositionLocalProvider(LocalChromeInsets provides insets) {
                    // Short, overlapping crossfade: the outgoing page leaves fast so rapid tab
                    // clicks never stack several pages in flight.
                    AnimatedContent(
                        targetState = currentPage,
                        transitionSpec = {
                            (fadeIn(tween(220, delayMillis = 40)) + scaleIn(tween(260, delayMillis = 40), initialScale = 0.985f)) togetherWith
                                fadeOut(tween(110))
                        },
                        label = "page"
                    ) { page ->
                        run {
                            when (page) {
                                Page.HOME -> HomePage()
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

                // ── Top bar ────────────────────────────────────────────────────────────────
                TopChrome(
                    compact = compact,
                    items = navItems,
                    current = currentPage,
                    onNavigate = navigate,
                    locale = currentLocale,
                    languageOpen = showLanguagePanel,
                    themeOpen = showThemePanel,
                    getAppLabel = site.getApp,
                    onLanguage = { showLanguagePanel = !showLanguagePanel; if (showLanguagePanel) showThemePanel = false },
                    onTheme = { showThemePanel = !showThemePanel; if (showThemePanel) showLanguagePanel = false },
                    a11yLanguage = strings.changeLanguageA11y,
                    a11yTheme = strings.customizeThemeA11y,
                )

                // ── Bottom floating toolbar (compact) ─────────────────────────────────────
                if (compact) {
                    BottomNavToolbar(
                        items = navItems,
                        current = currentPage,
                        onNavigate = navigate,
                        modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 16.dp).zIndex(50f)
                    )
                }

                // ── Overlay panels ───────────────────────────────────────────────────────
                val panelOpen = showLanguagePanel || showThemePanel
                if (panelOpen) {
                    Box(
                        Modifier.fillMaxSize().zIndex(150f).background(Color.Black.copy(alpha = 0.32f))
                            .clickable(remember { MutableInteractionSource() }, null) { showLanguagePanel = false; showThemePanel = false }
                    )
                }
                AnimatedVisibility(
                    visible = showLanguagePanel,
                    enter = fadeIn() + scaleIn(spring(0.7f, 500f), initialScale = 0.85f, transformOrigin = androidx.compose.ui.graphics.TransformOrigin(1f, 0f)),
                    exit = fadeOut(tween(120)) + scaleOut(targetScale = 0.9f, transformOrigin = androidx.compose.ui.graphics.TransformOrigin(1f, 0f)),
                    modifier = Modifier.align(Alignment.TopEnd).padding(top = insets.top - 8.dp, end = 16.dp).zIndex(200f)
                ) {
                    PanelSurface(Modifier.width(300.dp)) {
                        LanguagePanel(
                            currentLocale = currentLocale,
                            title = strings.languagePanelTitle,
                            closeLabel = strings.closeA11y,
                            onSelectLocale = { l -> if (l != currentLocale) withReveal { currentLocale = l; showLanguagePanel = false } else showLanguagePanel = false },
                            onClose = { showLanguagePanel = false }
                        )
                    }
                }
                AnimatedVisibility(
                    visible = showThemePanel,
                    enter = fadeIn() + scaleIn(spring(0.7f, 500f), initialScale = 0.85f, transformOrigin = androidx.compose.ui.graphics.TransformOrigin(1f, 0f)),
                    exit = fadeOut(tween(120)) + scaleOut(targetScale = 0.9f, transformOrigin = androidx.compose.ui.graphics.TransformOrigin(1f, 0f)),
                    modifier = Modifier.align(Alignment.TopEnd).padding(top = insets.top - 8.dp, end = 16.dp).zIndex(200f)
                ) {
                    PanelSurface(Modifier.width(340.dp)) {
                        ThemePanel(strings, site, controller, onClose = { showThemePanel = false })
                    }
                }

                // ── Theme / language reveal: the frozen previous frame, cut by a growing circle ──
                Box(
                    Modifier.fillMaxSize().zIndex(250f).drawWithContent {
                        val bmp = reveal.snapshot ?: return@drawWithContent
                        val p = reveal.progress.value
                        val o = reveal.origin.takeIf { it.isSpecified } ?: center
                        val maxR = kotlin.math.hypot(maxOf(o.x, size.width - o.x), maxOf(o.y, size.height - o.y))
                        val path = androidx.compose.ui.graphics.Path().apply {
                            addOval(androidx.compose.ui.geometry.Rect(o, maxR * p))
                        }
                        clipPath(path, androidx.compose.ui.graphics.ClipOp.Difference) { drawImage(bmp) }
                    }
                )

            }
        }
    }
}

@Composable
private fun PanelSurface(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    Surface(
        modifier = modifier.shadow(24.dp, RoundedCornerShape(32.dp)).clickable(remember { MutableInteractionSource() }, null) {},
        shape = RoundedCornerShape(32.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
    ) {
        Box(Modifier.padding(20.dp)) { content() }
    }
}

@Composable
private fun TopChrome(
    compact: Boolean,
    items: List<NavItem>,
    current: Page,
    onNavigate: (Page) -> Unit,
    locale: WebLocale,
    languageOpen: Boolean,
    themeOpen: Boolean,
    getAppLabel: String,
    onLanguage: () -> Unit,
    onTheme: () -> Unit,
    a11yLanguage: String,
    a11yTheme: String,
) {
    val cs = MaterialTheme.colorScheme
    val barColor = cs.surfaceContainer.copy(alpha = 0.94f)
    Row(
        Modifier.fillMaxWidth().zIndex(100f).padding(horizontal = if (compact) 12.dp else 20.dp, vertical = if (compact) 10.dp else 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // brand
        val brandSource = remember { MutableInteractionSource() }
        Row(
            Modifier
                .height(52.dp)
                .springyPress(brandSource, 0.95f, 1.03f)
                .shadow(8.dp, CircleShape)
                .clip(CircleShape)
                .background(barColor)
                .clickable(brandSource, null) { onNavigate(Page.HOME) }
                .padding(start = 8.dp, end = 18.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(Modifier.size(38.dp), contentAlignment = Alignment.Center) {
                MorphingBlob(Modifier.matchParentSize(), color = cs.primary, holdSeconds = 3f, morphSeconds = 1f, spinDegPerSecond = 8f)
                KittyLogo(Modifier.size(24.dp), tint = cs.onPrimary)
            }
            Text("KittyTune", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.W900), color = cs.onSurface)
        }

        Spacer(Modifier.weight(1f))

        if (!compact) {
            NavPill(items, current, onNavigate, barColor)
            Spacer(Modifier.weight(1f))
        }

        Row(
            Modifier.height(52.dp).shadow(8.dp, CircleShape).clip(CircleShape).background(barColor).padding(horizontal = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            val langBg by animateColorAsState(if (languageOpen) cs.primaryContainer else Color.Transparent)
            Row(
                Modifier.height(40.dp).clip(CircleShape).background(langBg).clickable(onClick = onLanguage).padding(horizontal = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(Icons.Rounded.Translate, a11yLanguage, tint = cs.onSurface, modifier = Modifier.size(20.dp))
                Text(locale.shortLabel, style = MaterialTheme.typography.labelLarge, color = cs.onSurface)
            }
            val themeBg by animateColorAsState(if (themeOpen) cs.primaryContainer else Color.Transparent)
            Box(
                Modifier.size(40.dp).clip(CircleShape).background(themeBg).clickable(onClick = onTheme),
                contentAlignment = Alignment.Center
            ) { Icon(Icons.Rounded.Palette, a11yTheme, tint = cs.onSurface, modifier = Modifier.size(20.dp)) }
            if (!compact) {
                val source = remember { MutableInteractionSource() }
                Button(
                    onClick = { onNavigate(Page.DOWNLOAD) },
                    shapes = ButtonDefaults.shapes(),
                    interactionSource = source,
                    contentPadding = PaddingValues(horizontal = 18.dp),
                    modifier = Modifier.height(40.dp),
                ) {
                    Icon(Icons.Rounded.Download, null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(getAppLabel, style = MaterialTheme.typography.labelLarge)
                }
            }
        }
    }
}

/** Desktop navigation: a pill with a springy sliding indicator under the active page. */
@Composable
private fun NavPill(items: List<NavItem>, current: Page, onNavigate: (Page) -> Unit, barColor: Color) {
    val cs = MaterialTheme.colorScheme
    val density = LocalDensity.current
    val positions = remember { mutableStateMapOf<Page, Pair<Float, Float>>() }
    val target = positions[current]
    val indicatorX by animateDpAsState(with(density) { (target?.first ?: 0f).toDp() }, spring(0.62f, 380f))
    val indicatorW by animateDpAsState(with(density) { (target?.second ?: 0f).toDp() }, spring(0.62f, 380f))
    Box(
        Modifier.height(52.dp).shadow(8.dp, CircleShape).clip(CircleShape).background(barColor).padding(6.dp)
    ) {
        if (target != null) {
            Box(Modifier.offset(x = indicatorX).width(indicatorW).fillMaxHeight().clip(CircleShape).background(cs.secondaryContainer))
        }
        Row(Modifier.fillMaxHeight(), verticalAlignment = Alignment.CenterVertically) {
            items.forEach { item ->
                val selected = item.page == current
                val fg by animateColorAsState(if (selected) cs.onSecondaryContainer else cs.onSurfaceVariant)
                val source = remember { MutableInteractionSource() }
                Row(
                    Modifier
                        .fillMaxHeight()
                        .onGloballyPositioned { c -> positions[item.page] = c.positionInParent().x to c.size.width.toFloat() }
                        .springyPress(source, 0.92f, 1f)
                        .clip(CircleShape)
                        .clickable(source, null) { onNavigate(item.page) }
                        .padding(horizontal = 18.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    AnimatedVisibility(selected, enter = fadeIn() + expandHorizontally(), exit = fadeOut() + shrinkHorizontally()) {
                        Icon(item.icon, null, tint = fg, modifier = Modifier.size(18.dp))
                    }
                    Text(item.label, style = MaterialTheme.typography.labelLarge, color = fg)
                }
            }
        }
    }
}

/** Compact navigation: a floating toolbar where the active item expands to show its label. */
@Composable
private fun BottomNavToolbar(items: List<NavItem>, current: Page, onNavigate: (Page) -> Unit, modifier: Modifier = Modifier) {
    val cs = MaterialTheme.colorScheme
    Row(
        modifier.shadow(16.dp, CircleShape).clip(CircleShape).background(cs.surfaceContainerHighest).padding(8.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        items.forEach { item ->
            val selected = item.page == current
            val bg by animateColorAsState(if (selected) cs.primary else Color.Transparent)
            val fg by animateColorAsState(if (selected) cs.onPrimary else cs.onSurfaceVariant)
            val source = remember { MutableInteractionSource() }
            Row(
                Modifier
                    .height(52.dp)
                    .springyPress(source, 0.88f, 1f)
                    .clip(CircleShape)
                    .background(bg)
                    .clickable(source, null) { onNavigate(item.page) }
                    .padding(horizontal = if (selected) 18.dp else 15.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(item.icon, item.label, tint = fg, modifier = Modifier.size(22.dp))
                AnimatedVisibility(selected, enter = fadeIn() + expandHorizontally(), exit = fadeOut() + shrinkHorizontally()) {
                    Text(item.label, style = MaterialTheme.typography.labelLarge, color = fg, maxLines = 1, modifier = Modifier.padding(start = 8.dp))
                }
            }
        }
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
    val cs = MaterialTheme.colorScheme
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        PanelHeader(Icons.Rounded.Translate, title, closeLabel, onClose)
        Column(
            verticalArrangement = Arrangement.spacedBy(4.dp),
            modifier = Modifier.heightIn(max = 440.dp).verticalScroll(rememberScrollState())
        ) {
            WebLocale.entries.forEach { locale ->
                val isSelected = locale == currentLocale
                val bg by animateColorAsState(if (isSelected) cs.primaryContainer else Color.Transparent)
                Row(
                    Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(bg).clickable { onSelectLocale(locale) }.padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        Modifier.size(36.dp).clip(PolygonShape(if (isSelected) MaterialShapes.Cookie9Sided else MaterialShapes.Circle))
                            .background(if (isSelected) cs.primary else cs.surfaceContainerHighest),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(locale.shortLabel, style = MaterialTheme.typography.labelMedium, color = if (isSelected) cs.onPrimary else cs.onSurfaceVariant)
                    }
                    Text(
                        locale.nativeName,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        color = if (isSelected) cs.onPrimaryContainer else cs.onSurface,
                        modifier = Modifier.weight(1f)
                    )
                    if (isSelected) Icon(Icons.Rounded.Check, null, tint = cs.onPrimaryContainer, modifier = Modifier.size(18.dp))
                }
            }
        }
    }
}

@Composable
private fun PanelHeader(icon: ImageVector, title: String, closeLabel: String, onClose: () -> Unit) {
    val cs = MaterialTheme.colorScheme
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Box(Modifier.size(36.dp).clip(PolygonShape(MaterialShapes.Sunny)).background(cs.primaryContainer), contentAlignment = Alignment.Center) {
            Icon(icon, null, tint = cs.onPrimaryContainer, modifier = Modifier.size(18.dp))
        }
        Text(title, style = MaterialTheme.typography.titleLarge, color = cs.onSurface, modifier = Modifier.weight(1f))
        IconButton(onClick = onClose, modifier = Modifier.size(36.dp)) {
            Icon(Icons.Rounded.Close, closeLabel, tint = cs.onSurfaceVariant, modifier = Modifier.size(18.dp))
        }
    }
}

@Composable
private fun ThemePanel(strings: AppChromeStrings, site: SiteStrings, controller: ThemeController, onClose: () -> Unit) {
    val cs = MaterialTheme.colorScheme
    Column(verticalArrangement = Arrangement.spacedBy(16.dp), modifier = Modifier.heightIn(max = 620.dp).verticalScroll(rememberScrollState())) {
        PanelHeader(Icons.Rounded.Palette, strings.customizeTitle, strings.closeA11y, onClose)

        // light / dark connected toggle
        Text(site.themeLabel, style = MaterialTheme.typography.labelLarge, color = cs.onSurfaceVariant)
        Row(horizontalArrangement = Arrangement.spacedBy(ButtonGroupDefaults.ConnectedSpaceBetween), modifier = Modifier.fillMaxWidth()) {
            ToggleButton(
                checked = !controller.isDark,
                onCheckedChange = { controller.setDark(false) },
                shapes = ButtonGroupDefaults.connectedLeadingButtonShapes(),
                modifier = Modifier.weight(1f).height(48.dp),
            ) {
                Icon(Icons.Rounded.LightMode, null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text(site.lightLabel)
            }
            ToggleButton(
                checked = controller.isDark,
                onCheckedChange = { controller.setDark(true) },
                shapes = ButtonGroupDefaults.connectedTrailingButtonShapes(),
                modifier = Modifier.weight(1f).height(48.dp),
            ) {
                Icon(Icons.Rounded.DarkMode, null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text(site.darkLabel)
            }
        }

        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(strings.seedColorLabel, style = MaterialTheme.typography.labelLarge, color = cs.onSurfaceVariant)
            Text(strings.colorName(controller.selectedColor.name), style = MaterialTheme.typography.labelLarge, color = cs.primary)
        }
        colorPresets.chunked(5).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                row.forEach { preset ->
                    val selected = controller.selectedColor.name == preset.name
                    val rot by androidx.compose.animation.core.animateFloatAsState(if (selected) 30f else 0f, spring(0.4f, 200f))
                    Box(
                        Modifier.size(48.dp).clip(PolygonShape(if (selected) MaterialShapes.Cookie9Sided else MaterialShapes.Circle, rot))
                            .background(preset.color).clickable { controller.selectColor(preset) },
                        contentAlignment = Alignment.Center
                    ) {
                        if (selected) Icon(Icons.Rounded.Check, strings.colorName(preset.name), tint = Color.White, modifier = Modifier.size(20.dp))
                    }
                }
            }
        }

        Text(strings.paletteStyleLabel, style = MaterialTheme.typography.labelLarge, color = cs.onSurfaceVariant)
        com.alananasss.kittytunewebsite.ui.kit.Wrap(hSpacing = 6.dp, vSpacing = 6.dp) {
            paletteStyles.forEach { style ->
                val selected = controller.selectedStyle.first == style.first
                ToggleButton(
                    checked = selected,
                    onCheckedChange = { controller.selectStyle(style) },
                    contentPadding = PaddingValues(horizontal = 14.dp),
                    modifier = Modifier.height(38.dp),
                ) {
                    Text(strings.paletteStyleName(style.first), style = MaterialTheme.typography.labelLarge)
                }
            }
        }
    }
}
