@file:OptIn(androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class, androidx.compose.ui.text.ExperimentalTextApi::class)

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.ImageComposeScene
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.platform.Font
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.materialkolor.PaletteStyle
import org.jetbrains.skia.EncodedImageFormat
import org.junit.Test
import java.io.File
import kotlin.math.PI
import kotlin.math.sin

class TmpInviteBannerRender {
    private val site = File("C:/Users/alan/Projects/kittytune-website")
    private val fontBytes = File(site, "src/wasmJsMain/composeResources/font/google_sans_flex.ttf").readBytes()

    private fun flex(tag: String, rond: Float, opsz: Float) = FontFamily(
        listOf(FontWeight.W400, FontWeight.W500, FontWeight.W600, FontWeight.W700, FontWeight.W800, FontWeight.W900).map { w ->
            Font("GSF-$tag-${w.weight}", fontBytes, w, variationSettings = FontVariation.Settings(
                FontVariation.weight(w.weight), FontVariation.width(100f),
                FontVariation.Setting("ROND", rond), FontVariation.Setting("opsz", opsz)))
        })

    private val display = flex("display", 100f, 72f)
    private val rounded = flex("rounded", 100f, 18f)
    private val text = flex("text", 0f, 14f)

    private fun img(path: String): ImageBitmap =
        org.jetbrains.skia.Image.makeFromEncoded(File(site, path).readBytes()).toComposeImageBitmap()

    @Test fun render() {
        val scheme = com.materialkolor.dynamicColorScheme(
            seedColor = Color(0xFF1A6DFF), isDark = true, isAmoled = false, style = PaletteStyle.Vibrant,
            specVersion = com.materialkolor.dynamiccolor.ColorSpec.SpecVersion.SPEC_2025,
        )
        val logo = img("tools/assets/kitty_logo.png")
        val cover = img("tools/assets/cover_black_hole.png")
        val scene = ImageComposeScene(1200, 630, Density(1f)) {
            CompositionLocalProvider(LocalDensity provides Density(1f)) {
                MaterialTheme(colorScheme = scheme) { Banner(logo, cover) }
            }
        }
        val out = File(site, "src/wasmJsMain/resources/together/invite.png")
        scene.render().use { it.encodeToData(EncodedImageFormat.PNG)!!.use { d -> out.writeBytes(d.bytes) } }
        scene.close()
        println("BANNER ${out.length()} bytes")
    }

    @Composable private fun Banner(logo: ImageBitmap, cover: ImageBitmap) {
        val s = MaterialTheme.colorScheme
        Box(Modifier.fillMaxSize().background(s.surfaceContainerLowest)) {
            // Soft light pools, like the site's hero.
            Canvas(Modifier.fillMaxSize()) {
                drawCircle(Brush.radialGradient(listOf(s.primary.copy(alpha = 0.30f), Color.Transparent), Offset(250f, 120f), 520f), 520f, Offset(250f, 120f))
                drawCircle(Brush.radialGradient(listOf(s.tertiary.copy(alpha = 0.22f), Color.Transparent), Offset(1000f, 560f), 520f), 520f, Offset(1000f, 560f))
            }
            // Big expressive shapes bleeding off the edges.
            Box(Modifier.offset((-90).dp, 430.dp).size(300.dp).rotate(18f).clip(MaterialShapes.SoftBurst.toShape()).background(s.primaryContainer.copy(alpha = 0.35f)))
            Box(Modifier.offset(560.dp, (-120).dp).size(220.dp).rotate(-12f).clip(MaterialShapes.Cookie9Sided.toShape()).background(s.tertiaryContainer.copy(alpha = 0.16f)))

            Row(Modifier.fillMaxSize().padding(horizontal = 60.dp, vertical = 54.dp)) {
                Column(Modifier.weight(1f).fillMaxHeight()) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.size(56.dp).clip(MaterialShapes.Cookie9Sided.toShape()).background(s.primaryContainer), contentAlignment = Alignment.Center) {
                            Image(logo, null, Modifier.size(40.dp), contentScale = ContentScale.Fit, filterQuality = FilterQuality.None)
                        }
                        Spacer(Modifier.width(14.dp))
                        Text("KittyTune", style = TextStyle(fontFamily = display, fontWeight = FontWeight.W800, fontSize = 30.sp, color = s.onSurface))
                        Spacer(Modifier.width(14.dp))
                        Pill(s.tertiaryContainer) {
                            Icon(Icons.Rounded.Headphones, null, Modifier.size(16.dp), tint = s.onTertiaryContainer)
                            Spacer(Modifier.width(6.dp))
                            Text("LISTEN TOGETHER", style = TextStyle(fontFamily = rounded, fontWeight = FontWeight.W700, fontSize = 13.sp, letterSpacing = 1.2.sp, color = s.onTertiaryContainer))
                        }
                    }
                    Spacer(Modifier.height(34.dp))
                    Text("You're invited", style = TextStyle(fontFamily = display, fontWeight = FontWeight.W900, fontSize = 76.sp, lineHeight = 78.sp, letterSpacing = (-2).sp, color = s.onSurface))
                    Text("to a listening party",
                        style = TextStyle(fontFamily = display, fontWeight = FontWeight.W900, fontSize = 58.sp, lineHeight = 66.sp, letterSpacing = (-1.5).sp,
                            brush = Brush.linearGradient(listOf(s.primary, s.tertiary))))
                    Spacer(Modifier.height(18.dp))
                    Text("Hop into a live session: same song, same second,\nlyrics in sync. Right in your browser or the app.",
                        style = TextStyle(fontFamily = text, fontWeight = FontWeight.W400, fontSize = 21.sp, lineHeight = 30.sp, color = s.onSurfaceVariant))
                    Spacer(Modifier.height(30.dp))
                    // The "ticket": live dot, call to action, and the join button.
                    Row(
                        Modifier.clip(CircleShape).background(s.surfaceContainerHigh).border(1.dp, s.outlineVariant.copy(alpha = 0.6f), CircleShape)
                            .padding(start = 22.dp, end = 8.dp, top = 8.dp, bottom = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Box(Modifier.size(12.dp).clip(CircleShape).background(Color(0xFF3DDC97)))
                        Spacer(Modifier.width(10.dp))
                        Text("LIVE NOW", style = TextStyle(fontFamily = rounded, fontWeight = FontWeight.W800, fontSize = 15.sp, letterSpacing = 1.sp, color = Color(0xFF3DDC97)))
                        Spacer(Modifier.width(16.dp))
                        Box(Modifier.width(1.dp).height(28.dp).background(s.outlineVariant))
                        Spacer(Modifier.width(16.dp))
                        Text("Open the link to join", style = TextStyle(fontFamily = rounded, fontWeight = FontWeight.W600, fontSize = 18.sp, color = s.onSurface))
                        Spacer(Modifier.width(18.dp))
                        Row(Modifier.clip(CircleShape).background(s.primary).padding(horizontal = 22.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text("Join", style = TextStyle(fontFamily = rounded, fontWeight = FontWeight.W800, fontSize = 18.sp, color = s.onPrimary))
                            Spacer(Modifier.width(8.dp))
                            Icon(Icons.AutoMirrored.Rounded.ArrowForward, null, Modifier.size(20.dp), tint = s.onPrimary)
                        }
                    }
                    Spacer(Modifier.weight(1f))
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Feature(Icons.Rounded.Favorite, "Free & open source")
                        Feature(Icons.Rounded.Lock, "End-to-end encrypted")
                        Feature(Icons.Rounded.Devices, "Android · Desktop · Web")
                    }
                }
                Spacer(Modifier.width(36.dp))
                NowPlayingCard(cover)
            }
        }
    }

    @Composable private fun Pill(color: Color, content: @Composable RowScope.() -> Unit) {
        Row(Modifier.clip(CircleShape).background(color).padding(horizontal = 14.dp, vertical = 7.dp), verticalAlignment = Alignment.CenterVertically, content = content)
    }

    @Composable private fun Feature(icon: ImageVector, label: String) {
        val s = MaterialTheme.colorScheme
        Row(Modifier.clip(CircleShape).background(s.surfaceContainer).padding(horizontal = 14.dp, vertical = 9.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, null, Modifier.size(16.dp), tint = s.primary)
            Spacer(Modifier.width(7.dp))
            Text(label, style = TextStyle(fontFamily = rounded, fontWeight = FontWeight.W600, fontSize = 14.sp, color = s.onSurfaceVariant))
        }
    }

    @Composable private fun NowPlayingCard(cover: ImageBitmap) {
        val s = MaterialTheme.colorScheme
        Column(
            Modifier.width(400.dp).fillMaxHeight().clip(RoundedCornerShape(40.dp)).background(s.surfaceContainer)
                .border(1.dp, s.outlineVariant.copy(alpha = 0.5f), RoundedCornerShape(40.dp)).padding(26.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(34.dp).clip(MaterialShapes.Cookie6Sided.toShape()).background(s.primary), contentAlignment = Alignment.Center) {
                    Icon(Icons.Rounded.Star, null, Modifier.size(18.dp), tint = s.onPrimary)
                }
                Spacer(Modifier.width(10.dp))
                Text("Host is playing", style = TextStyle(fontFamily = rounded, fontWeight = FontWeight.W700, fontSize = 16.sp, color = s.onSurface))
                Spacer(Modifier.weight(1f))
                Pill(Color(0xFF3DDC97).copy(alpha = 0.16f)) {
                    Box(Modifier.size(8.dp).clip(CircleShape).background(Color(0xFF3DDC97)))
                    Spacer(Modifier.width(6.dp))
                    Text("LIVE", style = TextStyle(fontFamily = rounded, fontWeight = FontWeight.W800, fontSize = 12.sp, letterSpacing = 1.sp, color = Color(0xFF3DDC97)))
                }
            }
            Spacer(Modifier.height(18.dp))
            Box(Modifier.size(230.dp), contentAlignment = Alignment.Center) {
                Box(Modifier.fillMaxSize().rotate(14f).clip(MaterialShapes.SoftBurst.toShape()).background(Brush.linearGradient(listOf(s.primary, s.tertiary))).alpha(0.9f))
                Image(cover, null, Modifier.size(186.dp).clip(MaterialShapes.Cookie12Sided.toShape()), contentScale = ContentScale.Crop)
            }
            Spacer(Modifier.height(16.dp))
            Text("Black Hole", style = TextStyle(fontFamily = display, fontWeight = FontWeight.W800, fontSize = 26.sp, color = s.onSurface))
            Text("Playing for everyone · Lossless", style = TextStyle(fontFamily = text, fontWeight = FontWeight.W500, fontSize = 15.sp, color = s.onSurfaceVariant))
            Spacer(Modifier.height(16.dp))
            // The expressive wavy progress line of the player.
            Canvas(Modifier.fillMaxWidth().height(22.dp)) {
                val played = size.width * 0.46f
                val mid = size.height / 2
                val wave = Path().apply {
                    moveTo(0f, mid)
                    var x = 0f
                    while (x <= played) { lineTo(x, mid + sin(x / 34f * 2 * PI).toFloat() * 5f); x += 2f }
                }
                drawPath(wave, s.primary, style = Stroke(width = 6f, cap = StrokeCap.Round))
                drawLine(s.surfaceContainerHighest, Offset(played + 12f, mid), Offset(size.width, mid), strokeWidth = 6f, cap = StrokeCap.Round)
                drawCircle(s.primary, 9f, Offset(played + 2f, mid))
            }
            Spacer(Modifier.weight(1f))
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                val avatars = listOf(s.primary, s.tertiary, s.secondary, s.error)
                Box(Modifier.height(36.dp).width((36 + 24 * 3).dp)) {
                    avatars.forEachIndexed { i, c ->
                        Box(Modifier.offset((i * 24).dp).size(36.dp).clip(CircleShape).background(s.surfaceContainer).padding(3.dp).clip(CircleShape).background(c),
                            contentAlignment = Alignment.Center) {
                            Text(listOf("A", "M", "K", "J")[i], style = TextStyle(fontFamily = rounded, fontWeight = FontWeight.W800, fontSize = 14.sp, color = s.surface))
                        }
                    }
                }
                Spacer(Modifier.width(12.dp))
                Column {
                    Text("4 listening", style = TextStyle(fontFamily = rounded, fontWeight = FontWeight.W700, fontSize = 16.sp, color = s.onSurface))
                    Text("in perfect sync", style = TextStyle(fontFamily = text, fontSize = 13.sp, color = s.onSurfaceVariant))
                }
                Spacer(Modifier.weight(1f))
                Icon(Icons.Rounded.GraphicEq, null, Modifier.size(30.dp), tint = s.tertiary)
            }
        }
    }
}
