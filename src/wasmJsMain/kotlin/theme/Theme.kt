package com.alananasss.kittytunewebsite.ui.theme

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.LocalScrollbarStyle
import androidx.compose.foundation.defaultScrollbarStyle
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.setValue
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialExpressiveTheme
import androidx.compose.material3.MotionScheme
import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.platform.Font
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.materialkolor.PaletteStyle
import com.materialkolor.dynamiccolor.ColorSpec
import com.materialkolor.rememberDynamicColorScheme
import kittytunewebsite.generated.resources.Res

/** Extra type families that the stock M3 [Typography] cannot express (wide display cut, etc.). */
data class KittyFonts(
    val display: FontFamily = FontFamily.Default,
    val rounded: FontFamily = FontFamily.Default,
    val text: FontFamily = FontFamily.Default,
)

val LocalKittyFonts = staticCompositionLocalOf { KittyFonts() }

/** True when the site is rendered with the dark scheme. */
val LocalIsDark = staticCompositionLocalOf { true }

private val weights = listOf(
    FontWeight.W400, FontWeight.W500, FontWeight.W600,
    FontWeight.W700, FontWeight.W800, FontWeight.W900,
)

@OptIn(ExperimentalTextApi::class)
private fun flexFamily(bytes: ByteArray, tag: String, rond: Float, width: Float, opsz: Float, ws: List<FontWeight> = weights) =
    FontFamily(
        ws.map { fw ->
            Font(
                identity = "KittyTuneFlex-$tag-${fw.weight}",
                data = bytes,
                weight = fw,
                variationSettings = FontVariation.Settings(
                    FontVariation.weight(fw.weight),
                    FontVariation.width(width),
                    FontVariation.Setting("ROND", rond),
                    FontVariation.Setting("opsz", opsz),
                )
            )
        }
    )

private fun expressiveTypography(rounded: FontFamily, text: FontFamily, display: FontFamily): Typography {
    val d = Typography()
    fun TextStyle.r() = copy(fontFamily = rounded)
    fun TextStyle.t() = copy(fontFamily = text)
    return Typography(
        displayLarge = d.displayLarge.copy(fontFamily = display, fontWeight = FontWeight.W900, fontSize = 64.sp, lineHeight = 68.sp, letterSpacing = (-1.5).sp),
        displayMedium = d.displayMedium.copy(fontFamily = display, fontWeight = FontWeight.W800, fontSize = 50.sp, lineHeight = 56.sp, letterSpacing = (-1).sp),
        displaySmall = d.displaySmall.copy(fontFamily = display, fontWeight = FontWeight.W800, fontSize = 38.sp, lineHeight = 44.sp, letterSpacing = (-0.5).sp),
        headlineLarge = d.headlineLarge.r().copy(fontWeight = FontWeight.W800),
        headlineMedium = d.headlineMedium.r().copy(fontWeight = FontWeight.W700),
        headlineSmall = d.headlineSmall.r().copy(fontWeight = FontWeight.W700),
        titleLarge = d.titleLarge.r().copy(fontWeight = FontWeight.W700),
        titleMedium = d.titleMedium.r().copy(fontWeight = FontWeight.W700),
        titleSmall = d.titleSmall.r().copy(fontWeight = FontWeight.W600),
        bodyLarge = d.bodyLarge.t().copy(lineHeight = 26.sp),
        bodyMedium = d.bodyMedium.t().copy(lineHeight = 22.sp),
        bodySmall = d.bodySmall.t(),
        labelLarge = d.labelLarge.r().copy(fontWeight = FontWeight.W700),
        labelMedium = d.labelMedium.r().copy(fontWeight = FontWeight.W600),
        labelSmall = d.labelSmall.r().copy(fontWeight = FontWeight.W600),
    )
}

private val schemeCache = HashMap<Triple<Int, Boolean, PaletteStyle>, ColorScheme>()

/**
 * The Material You scheme for a seed. Generating one (HCT maths in materialkolor) costs tens of
 * milliseconds, so every scheme is built once and kept; the app pre-builds the presets while idle so
 * switching colour or dark mode never has to compute anything.
 */
fun kittyScheme(seedColor: Color, isDark: Boolean, style: PaletteStyle): ColorScheme =
    schemeCache.getOrPut(Triple(seedColor.toArgb(), isDark, style)) {
        com.materialkolor.dynamicColorScheme(
            seedColor = seedColor,
            isDark = isDark,
            isAmoled = false,
            style = style,
            specVersion = ColorSpec.SpecVersion.SPEC_2025,
        )
    }

@OptIn(ExperimentalMaterial3ExpressiveApi::class, ExperimentalTextApi::class)
@Composable
fun KittyTuneWebTheme(
    seedColor: Color = Color(0xFF1A6DFF),
    paletteStyle: PaletteStyle = PaletteStyle.Vibrant,
    isDark: Boolean = true,
    content: @Composable () -> Unit
) {
    val rawScheme = androidx.compose.runtime.remember(seedColor, isDark, paletteStyle) { kittyScheme(seedColor, isDark, paletteStyle) }
    // Not animated colour by colour: see the snapshot reveal in App.kt.
    val colorScheme = rawScheme

    var fontLoadFailed by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(false) }
    val fontBytes by produceState<ByteArray?>(null) {
        value = runCatching { Res.readBytes("font/google_sans_flex.ttf") }.getOrNull()
        if (value == null) fontLoadFailed = true
    }
    // Google Sans Flex has no Cyrillic; Nunito (rounded, variable) fills those glyphs so Russian
    // never waits on Compose's lazy Noto download from fonts.gstatic.com.
    val fallbackBytes by produceState<ByteArray?>(null) {
        value = runCatching { Res.readBytes("font/nunito.ttf") }.getOrNull() ?: ByteArray(0)
    }
    val fallbackFamily = remember(fallbackBytes) {
        val b = fallbackBytes
        if (b == null || b.isEmpty()) null
        else FontFamily(
            weights.map { fw ->
                Font(
                    identity = "Nunito-${fw.weight}",
                    data = b,
                    weight = fw,
                    variationSettings = FontVariation.Settings(FontVariation.weight(fw.weight)),
                )
            }
        )
    }

    val fonts = remember(fontBytes) {
        val bytes = fontBytes ?: return@remember null
        KittyFonts(
            display = flexFamily(bytes, "display", rond = 100f, width = 100f, opsz = 72f, ws = listOf(FontWeight.W700, FontWeight.W800, FontWeight.W900)),
            rounded = flexFamily(bytes, "rounded", rond = 100f, width = 100f, opsz = 18f),
            text = flexFamily(bytes, "text", rond = 0f, width = 100f, opsz = 14f, ws = listOf(FontWeight.W400, FontWeight.W500, FontWeight.W600, FontWeight.W700)),
        )
    }
    // Text laid out before every variable instance is registered keeps tofu glyphs (non-Latin
    // scripts) in its cached paragraphs, so nothing is composed until all instances are preloaded.
    val resolver = androidx.compose.ui.platform.LocalFontFamilyResolver.current
    val fontsReady by produceState(false, fonts, fallbackBytes) {
        val f = fonts
        if (f == null || fallbackBytes == null) {
            value = false
        } else {
            // One typeface per frame: parsing ~19 variable-font instances in a single task froze the
            // page (and the splash) for ~800 ms. Spread out, each frame stays short; preloading the
            // full families afterwards only hits the cache.
            val families = listOfNotNull(fallbackFamily, f.display, f.rounded, f.text)
            for (family in families) {
                val fonts = (family as? androidx.compose.ui.text.font.FontListFontFamily)?.fonts ?: continue
                for (font in fonts) {
                    runCatching { resolver.preload(FontFamily(font)) }
                    androidx.compose.runtime.withFrameNanos { }
                }
            }
            runCatching { families.forEach { resolver.preload(it) } }
            value = true
        }
    }
    val typography = remember(fonts) {
        if (fonts == null) expressiveTypography(FontFamily.Default, FontFamily.Default, FontFamily.Default)
        else expressiveTypography(fonts.rounded, fonts.text, fonts.display)
    }

    val scrollbarStyle = defaultScrollbarStyle().copy(
        thickness = 8.dp,
        shape = RoundedCornerShape(4.dp),
        hoverColor = colorScheme.primary.copy(alpha = 0.8f),
        unhoverColor = colorScheme.onSurfaceVariant.copy(alpha = 0.35f),
        hoverDurationMillis = 300
    )

    CompositionLocalProvider(
        LocalScrollbarStyle provides scrollbarStyle,
        LocalKittyFonts provides (fonts ?: KittyFonts()),
        LocalIsDark provides isDark,
    ) {
        MaterialExpressiveTheme(
            colorScheme = colorScheme,
            motionScheme = MotionScheme.expressive(),
            typography = typography,
        ) {
            if (fontsReady || fontBytes == null && fontLoadFailed) content()
            else androidx.compose.foundation.layout.Box(
                androidx.compose.ui.Modifier.fillMaxSize().background(colorScheme.surface)
            )
        }
    }
}
