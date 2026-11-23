@file:OptIn(androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class, androidx.compose.ui.text.ExperimentalTextApi::class)

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.Headphones
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Sync
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.ImageComposeScene
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toComposeImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
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

/**
 * The Listen Together invite banner (1200x630, the size link previews ask for), built like the website's
 * hero: the site's own dark scheme and type, real Material 3 components (Surface, Button with expressive
 * shapes), and its stage of a Cookie12 and a Sunny behind the real app.
 */
class TmpInviteBannerRender {
    private val site = File("C:/Users/alan/Projects/kittytune-website")
    private val fontBytes = File(site, "src/wasmJsMain/composeResources/font/google_sans_flex.ttf").readBytes()

    private fun flex(tag: String, rond: Float, opsz: Float) = FontFamily(
        listOf(FontWeight.W400, FontWeight.W500, FontWeight.W600, FontWeight.W700, FontWeight.W800, FontWeight.W900).map { w ->
            Font("GSF-$tag-${w.weight}", fontBytes, w, variationSettings = FontVariation.Settings(
                FontVariation.weight(w.weight), FontVariation.width(100f),
                FontVariation.Setting("ROND", rond), FontVariation.Setting("opsz", opsz)))
        })

    /** The website's expressive type scale (theme/Theme.kt). */
    private fun typography(): Typography {
        val display = flex("display", 100f, 72f)
        val rounded = flex("rounded", 100f, 18f)
        val text = flex("text", 0f, 14f)
        val d = Typography()
        return Typography(
            displayLarge = d.displayLarge.copy(fontFamily = display, fontWeight = FontWeight.W900, fontSize = 64.sp, lineHeight = 68.sp, letterSpacing = (-1.5).sp),
            titleLarge = d.titleLarge.copy(fontFamily = rounded, fontWeight = FontWeight.W700),
            titleMedium = d.titleMedium.copy(fontFamily = rounded, fontWeight = FontWeight.W700),
            bodyLarge = d.bodyLarge.copy(fontFamily = text, lineHeight = 26.sp),
            labelLarge = d.labelLarge.copy(fontFamily = rounded, fontWeight = FontWeight.W700),
        )
    }

    private fun img(path: String): ImageBitmap =
        org.jetbrains.skia.Image.makeFromEncoded(File(site, path).readBytes()).toComposeImageBitmap()

    @Test fun render() {
        // Exactly the site's default scheme: seed #1A6DFF, Vibrant, dark, 2025 spec.
        val scheme = com.materialkolor.dynamicColorScheme(
            seedColor = androidx.compose.ui.graphics.Color(0xFF1A6DFF), isDark = true, isAmoled = false,
            style = PaletteStyle.Vibrant, specVersion = com.materialkolor.dynamiccolor.ColorSpec.SpecVersion.SPEC_2025,
        )
        val phone = img("src/wasmJsMain/composeResources/drawable/hero_player.webp")
        val scene = ImageComposeScene(1200, 630, Density(1f)) {
            CompositionLocalProvider(LocalDensity provides Density(1f)) {
                MaterialTheme(colorScheme = scheme, typography = typography()) { Banner(phone) }
            }
        }
        val out = File(site, "src/wasmJsMain/resources/together/invite.png")
        scene.render().use { it.encodeToData(EncodedImageFormat.PNG)!!.use { d -> out.writeBytes(d.bytes) } }
        scene.close()
        println("BANNER ${out.length()} bytes")
    }

    @Composable private fun Banner(phone: ImageBitmap) {
        val cs = MaterialTheme.colorScheme
        val type = MaterialTheme.typography
        Surface(Modifier.fillMaxSize(), color = cs.surface) {
            Box(Modifier.fillMaxSize()) {
                Stage(phone, Modifier.align(Alignment.CenterEnd).offset(x = 40.dp).width(560.dp).fillMaxHeight())

                Column(
                    Modifier.align(Alignment.CenterStart).padding(start = 72.dp).width(600.dp),
                    verticalArrangement = Arrangement.spacedBy(28.dp),
                ) {
                    // The site's eyebrow pill.
                    Surface(shape = CircleShape, color = cs.secondaryContainer) {
                        Row(Modifier.padding(start = 12.dp, end = 18.dp, top = 9.dp, bottom = 9.dp),
                            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Icon(Icons.Rounded.Headphones, null, tint = cs.onSecondaryContainer, modifier = Modifier.size(20.dp))
                            Text("KittyTune Together", style = type.labelLarge.copy(fontSize = 16.sp), color = cs.onSecondaryContainer)
                        }
                    }
                    // The site's hero title: two lines, the second in primary.
                    Column {
                        val title = type.displayLarge.copy(fontSize = 92.sp, lineHeight = 94.sp, letterSpacing = (-2.5).sp)
                        Text("Listen along,", style = title, color = cs.onSurface)
                        Text("right now.", style = title, color = cs.primary)
                    }
                    Text(
                        "A live session is waiting for you.",
                        style = type.titleMedium.copy(fontSize = 24.sp, fontWeight = FontWeight.W500, lineHeight = 32.sp),
                        color = cs.onSurfaceVariant,
                    )
                    // A real Material 3 Expressive button, sized like the site's hero CTA.
                    Button(
                        onClick = {},
                        shapes = ButtonDefaults.shapes(),
                        contentPadding = PaddingValues(horizontal = 32.dp),
                        modifier = Modifier.height(68.dp),
                    ) {
                        Icon(Icons.Rounded.PlayArrow, null, modifier = Modifier.size(26.dp))
                        Spacer(Modifier.width(12.dp))
                        Text("Join the session", style = type.titleMedium.copy(fontSize = 21.sp))
                    }
                }
            }
        }
    }

    /** The home page's hero stage: a Cookie12 in primaryContainer, a Sunny in tertiaryContainer, the real app. */
    @Composable private fun Stage(phone: ImageBitmap, modifier: Modifier) {
        val cs = MaterialTheme.colorScheme
        Box(modifier, contentAlignment = Alignment.Center) {
            Box(Modifier.offset(y = 40.dp).size(520.dp).rotate(12f).clip(MaterialShapes.Cookie12Sided.toShape()).background(cs.primaryContainer))
            Box(Modifier.align(Alignment.TopEnd).offset(x = (-70).dp, y = 34.dp).size(118.dp).rotate(-10f)
                .clip(MaterialShapes.Sunny.toShape()).background(cs.tertiaryContainer))
            // The phone rises from the bottom edge, like a phone held up to show a friend.
            Image(phone, "KittyTune now playing", Modifier.offset(x = (-10).dp, y = 150.dp).height(760.dp), contentScale = ContentScale.FillHeight)
            FloatingBadge("Live", Icons.Rounded.GraphicEq, Modifier.align(Alignment.CenterStart).offset(x = 6.dp, y = (-96).dp))
            FloatingBadge("In sync", Icons.Rounded.Sync, Modifier.align(Alignment.CenterEnd).offset(x = (-58).dp, y = 70.dp))
        }
    }

    /** The site's floating badge: a pill Surface with a Cookie9 icon. */
    @Composable private fun FloatingBadge(text: String, icon: ImageVector, modifier: Modifier) {
        val cs = MaterialTheme.colorScheme
        Surface(modifier, shape = CircleShape, color = cs.surfaceContainerHighest, shadowElevation = 6.dp) {
            Row(Modifier.padding(start = 9.dp, end = 18.dp, top = 9.dp, bottom = 9.dp),
                verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Box(Modifier.size(34.dp).clip(MaterialShapes.Cookie9Sided.toShape()).background(cs.primary), contentAlignment = Alignment.Center) {
                    Icon(icon, null, tint = cs.onPrimary, modifier = Modifier.size(20.dp))
                }
                Text(text, style = MaterialTheme.typography.labelLarge.copy(fontSize = 18.sp), color = cs.onSurface)
            }
        }
    }
}
