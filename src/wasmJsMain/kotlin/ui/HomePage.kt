@file:OptIn(ExperimentalMaterial3ExpressiveApi::class)

package com.alananasss.kittytunewebsite.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.alananasss.kittytunewebsite.ui.demos.*
import com.alananasss.kittytunewebsite.ui.kit.*
import com.alananasss.kittytunewebsite.ui.pet.PetMood
import com.alananasss.kittytunewebsite.ui.pet.PixelPet
import kittytunewebsite.generated.resources.*
import kotlinx.coroutines.delay
import org.jetbrains.compose.resources.DrawableResource

const val RepoAndroid = "https://github.com/alan7383/kittytune"
const val RepoDesktop = "https://github.com/alan7383/KittyTuneDesktop"
const val RepoWebsite = "https://github.com/alan7383/kittytune-website"
const val KofiUrl = "https://ko-fi.com/alan7383"

fun openUrl(url: String) {
    kotlinx.browser.window.open(url, "_blank")
}

@Composable
fun HomePage() {
    val state = androidx.compose.foundation.lazy.rememberLazyListState()
    LazyPageColumn(state) {
        item(key = "hero") { HeroSection() }
        item(key = "marquee") { Spacer(Modifier.height(40.dp)); MarqueeBands() }
        item(key = "stats") { Spacer(Modifier.height(120.dp)); StatsSection() }
        item(key = "platforms") { Spacer(Modifier.height(140.dp)); PlatformsSection() }
        item(key = "linux") { Spacer(Modifier.height(140.dp)); LinuxSection() }
        item(key = "advantages") { Spacer(Modifier.height(140.dp)); AdvantagesSection() }
        item(key = "bento-head") { Spacer(Modifier.height(140.dp)); BentoHeading() }
        // One lazy item per bento row: a row of live demos is composed only when it comes near.
        items(BentoRowCount, key = { "bento-$it" }) { BentoRow(it) }
        item(key = "unblock") { Spacer(Modifier.height(140.dp)); UnblockSection() }
        item(key = "screens") { Spacer(Modifier.height(140.dp)); RealScreensSection() }
        item(key = "compare") { Spacer(Modifier.height(140.dp)); CompareSection() }
        item(key = "integrations") { Spacer(Modifier.height(140.dp)); IntegrationsSection() }
        item(key = "cta") { Spacer(Modifier.height(140.dp)); FinalCta() }
        item(key = "footer") { Spacer(Modifier.height(80.dp)); SiteFooter() }
    }
}

// ───────────────────────────────────────────── Hero ─────────────────────────────────────────────

@Composable
private fun HeroSection() {
    val s = site()
    val cs = MaterialTheme.colorScheme
    val navigate = LocalNavigate.current
    ViewportGate {
        ContentWidth {
            val wide = maxWidth >= 1080.dp
            val compact = maxWidth < 640.dp
            val heroText: @Composable (Modifier) -> Unit = { m ->
                Column(m, verticalArrangement = Arrangement.spacedBy(if (compact) 20.dp else 28.dp)) {
                    Eyebrow(s.heroEyebrow, Icons.Rounded.AutoAwesome, modifier = Modifier.reveal(0))
                    HeroTitle(s.heroLine1, s.heroLine2, compact)
                    Text(
                        s.heroSubtitle,
                        style = if (compact) MaterialTheme.typography.bodyLarge else MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.W500, lineHeight = 30.sp),
                        color = cs.onSurfaceVariant,
                        modifier = Modifier.widthIn(max = 580.dp).reveal(260)
                    )
                    Wrap(Modifier.reveal(360), hSpacing = 12.dp, vSpacing = 12.dp) {
                        Button(
                            onClick = { navigate(Page.DOWNLOAD) },
                            shapes = ButtonDefaults.shapes(),
                            contentPadding = PaddingValues(horizontal = 30.dp),
                            modifier = Modifier.height(64.dp),
                        ) {
                            Icon(Icons.Rounded.Download, null, modifier = Modifier.size(22.dp))
                            Spacer(Modifier.width(12.dp))
                            Text(s.ctaDownload, style = MaterialTheme.typography.titleMedium)
                        }
                        FilledTonalButton(
                            onClick = { openUrl(RepoDesktop) },
                            shapes = ButtonDefaults.shapes(),
                            contentPadding = PaddingValues(horizontal = 26.dp),
                            modifier = Modifier.height(64.dp),
                        ) {
                            Icon(Icons.Rounded.Code, null, modifier = Modifier.size(22.dp))
                            Spacer(Modifier.width(12.dp))
                            Text(s.ctaSource, style = MaterialTheme.typography.titleMedium)
                        }
                    }
                    Wrap(Modifier.reveal(460), hSpacing = 8.dp, vSpacing = 8.dp) {
                        s.heroTrust.forEach { TagPill(it, icon = Icons.Rounded.Check, color = cs.surfaceContainerHigh, content = cs.onSurface) }
                    }
                }
            }
            if (wide) {
                Row(Modifier.fillMaxWidth().padding(top = 32.dp, bottom = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                    heroText(Modifier.weight(1f))
                    Spacer(Modifier.width(40.dp))
                    HeroStage(Modifier.weight(0.95f), height = 660.dp)
                }
            } else {
                Column(Modifier.fillMaxWidth().padding(top = 20.dp), verticalArrangement = Arrangement.spacedBy(40.dp)) {
                    heroText(Modifier.fillMaxWidth())
                    HeroStage(Modifier.fillMaxWidth(), height = if (compact) 600.dp else 660.dp)
                }
            }
        }
    }
}

@Composable
private fun HeroTitle(line1: String, line2: String, compact: Boolean) {
    val cs = MaterialTheme.colorScheme
    val base = MaterialTheme.typography.displayLarge.let {
        if (compact) it.copy(fontSize = 54.sp, lineHeight = 58.sp, letterSpacing = (-1).sp) else it.copy(fontSize = 86.sp, lineHeight = 90.sp, letterSpacing = (-2.5).sp)
    }
    Column {
        Text(line1, style = base, color = cs.onSurface, modifier = Modifier.reveal(80, distance = 80f))
        Text(line2, style = base, color = cs.primary, modifier = Modifier.reveal(160, distance = 80f))
    }
}

/** The real app on the official Pixel 10 frame, over a slowly turning expressive shape. */
@Composable
private fun HeroStage(modifier: Modifier, height: Dp) {
    val cs = MaterialTheme.colorScheme
    val clock = rememberClock()
    Box(modifier.height(height).reveal(220, distance = 120f), contentAlignment = Alignment.Center) {
        Box(
            Modifier.size(height * 0.88f).graphicsLayer { rotationZ = clock.value * 5f }
                .clip(PolygonShape(MaterialShapes.Cookie12Sided)).background(cs.primaryContainer)
        )
        Box(
            Modifier.align(Alignment.TopEnd).padding(top = height * 0.06f).size(height * 0.18f)
                .graphicsLayer { rotationZ = -clock.value * 12f }
                .clip(PolygonShape(MaterialShapes.Sunny)).background(cs.tertiaryContainer)
        )
        Screenshot(
            Res.drawable.hero_player, "KittyTune playing ural mountains",
            Modifier.height(height * 0.96f).graphicsLayer { translationY = wave(clock.value, 7f) * 6f }
        )
        FloatingBadge("Lossless", Icons.Rounded.Verified, Modifier.align(Alignment.CenterStart).padding(bottom = height * 0.32f), 0f)
        FloatingBadge("Lyrics", Icons.Rounded.Lyrics, Modifier.align(Alignment.CenterEnd).padding(top = height * 0.38f), 0.5f)
    }
}

@Composable
private fun FloatingBadge(text: String, icon: ImageVector, modifier: Modifier, phase: Float) {
    val cs = MaterialTheme.colorScheme
    val clock = rememberClock()
    Surface(
        modifier = modifier.graphicsLayer { translationY = wave(clock.value, 4.5f, phase) * 8f },
        shape = CircleShape,
        color = cs.surfaceContainerHighest,
        shadowElevation = 6.dp,
    ) {
        Row(Modifier.padding(start = 8.dp, end = 14.dp, top = 8.dp, bottom = 8.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Box(Modifier.size(28.dp).clip(PolygonShape(MaterialShapes.Cookie9Sided)).background(cs.primary), contentAlignment = Alignment.Center) {
                Icon(icon, null, tint = cs.onPrimary, modifier = Modifier.size(16.dp))
            }
            Text(text, style = MaterialTheme.typography.labelLarge, color = cs.onSurface)
        }
    }
}

// ─────────────────────────────────────────── Marquee ───────────────────────────────────────────

@Composable
private fun MarqueeBands() {
    val s = site()
    val cs = MaterialTheme.colorScheme
    ViewportGate {
        Box(Modifier.fillMaxWidth().height(170.dp), contentAlignment = Alignment.Center) {
            Box(Modifier.fillMaxWidth(1.1f).graphicsLayer { rotationZ = 2.2f }.background(cs.tertiaryContainer).padding(vertical = 16.dp)) {
                Marquee(speedDpPerSecond = 36f, reverse = true) {
                    s.marquee.reversed().forEach { MarqueeItem(it, cs.onTertiaryContainer) }
                }
            }
            Box(Modifier.fillMaxWidth(1.1f).graphicsLayer { rotationZ = -2.2f }.background(cs.primary).padding(vertical = 18.dp)) {
                Marquee(speedDpPerSecond = 52f) {
                    s.marquee.forEach { MarqueeItem(it, cs.onPrimary) }
                }
            }
        }
    }
}

@Composable
private fun MarqueeItem(text: String, color: Color) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(text, style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.W800), color = color, maxLines = 1)
        Box(Modifier.padding(horizontal = 22.dp).size(18.dp).clip(PolygonShape(MaterialShapes.SoftBurst)).background(color.copy(alpha = 0.8f)))
    }
}

// ──────────────────────────────────────────── Stats ────────────────────────────────────────────

@Composable
private fun StatsSection() {
    val s = site()
    val cs = MaterialTheme.colorScheme
    val shapes = listOf(MaterialShapes.Cookie9Sided, MaterialShapes.Sunny, MaterialShapes.Clover4Leaf, MaterialShapes.Puffy, MaterialShapes.Gem, MaterialShapes.SoftBurst)
    val containers = listOf(cs.primaryContainer, cs.tertiaryContainer, cs.secondaryContainer)
    val inks = listOf(cs.onPrimaryContainer, cs.onTertiaryContainer, cs.onSecondaryContainer)
    ContentWidth {
        val cols = when { maxWidth < 560.dp -> 2; maxWidth < 980.dp -> 3; else -> 6 }
        ViewportGate {
            val clock = rememberClock()
            Column(verticalArrangement = Arrangement.spacedBy(24.dp)) {
                s.stats.chunked(cols).forEachIndexed { r, row ->
                    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        row.forEachIndexed { c, (num, label) ->
                            val i = r * cols + c
                            Column(Modifier.weight(1f).reveal(i * 70), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                Box(Modifier.size(120.dp), contentAlignment = Alignment.Center) {
                                    Box(Modifier.matchParentSize().spinningPolygon(shapes[i % shapes.size], containers[i % 3], clock, if (i % 2 == 0) 8f else -8f))
                                    Text(num, style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.W900), color = inks[i % 3], textAlign = TextAlign.Center)
                                }
                                Text(label, style = MaterialTheme.typography.titleSmall, color = cs.onSurfaceVariant, textAlign = TextAlign.Center)
                            }
                        }
                        repeat(cols - row.size) { Spacer(Modifier.weight(1f)) }
                    }
                }
            }
        }
    }
}

// ────────────────────────────────────────── Platforms ──────────────────────────────────────────

@Composable
private fun PlatformsSection() {
    val s = site()
    val cs = MaterialTheme.colorScheme
    ContentWidth {
        val wide = maxWidth >= 980.dp
        Column(verticalArrangement = Arrangement.spacedBy(48.dp)) {
            SectionHeading(s.platformsEyebrow, s.platformsTitle, s.platformsSubtitle, Modifier.reveal(), eyebrowIcon = Icons.Rounded.Devices)
            ViewportGate {
                val cards: List<@Composable (Modifier) -> Unit> = listOf(
                    { m ->
                        PlatformCard(m, Icons.Rounded.PhoneAndroid, s.androidTitle, s.androidDesc, cs.primaryContainer, cs.onPrimaryContainer, MaterialShapes.Cookie9Sided) {
                            Row(horizontalArrangement = Arrangement.spacedBy((-36).dp), verticalAlignment = Alignment.Bottom) {
                                Screenshot(Res.drawable.android_homescreen, "Android home", Modifier.height(300.dp).graphicsLayer { rotationZ = -6f; translationY = 14f })
                                Screenshot(Res.drawable.android_playerscreen, "Android player", Modifier.height(320.dp).zIndex(1f))
                                Screenshot(Res.drawable.android_lyrics, "Android lyrics", Modifier.height(300.dp).graphicsLayer { rotationZ = 6f; translationY = 14f })
                            }
                        }
                    },
                    { m ->
                        PlatformCard(m, Icons.Rounded.DesktopWindows, s.desktopTitle, s.desktopDesc, cs.tertiaryContainer, cs.onTertiaryContainer, MaterialShapes.Sunny) {
                            Screenshot(
                                Res.drawable.desk_home, "Desktop home",
                                Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).border(1.dp, cs.outlineVariant, RoundedCornerShape(16.dp)),
                                contentScale = ContentScale.FillWidth
                            )
                        }
                    },
                    { m ->
                        PlatformCard(m, Icons.Rounded.Watch, s.wearTitle, s.wearDesc, cs.secondaryContainer, cs.onSecondaryContainer, MaterialShapes.Clover4Leaf) {
                            WearDemo(size = 190.dp)
                        }
                    },
                )
                if (wide) {
                    Row(Modifier.fillMaxWidth().height(640.dp), horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                        cards.forEachIndexed { i, c -> c(Modifier.weight(if (i == 1) 1.3f else 1f).fillMaxHeight().reveal(i * 100)) }
                    }
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(20.dp)) {
                        cards.forEachIndexed { i, c -> c(Modifier.fillMaxWidth().reveal(i * 80)) }
                    }
                }
            }
        }
    }
}

@Composable
private fun PlatformCard(
    modifier: Modifier,
    icon: ImageVector,
    title: String,
    desc: String,
    container: Color,
    ink: Color,
    polygon: androidx.graphics.shapes.RoundedPolygon,
    visual: @Composable () -> Unit,
) {
    val cs = MaterialTheme.colorScheme
    BentoCard(modifier, color = cs.surfaceContainerLow, shape = RoundedCornerShape(40.dp)) {
        Column(Modifier.padding(28.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            ShapeIcon(icon, polygon, 56.dp, container, ink)
            Text(title, style = MaterialTheme.typography.headlineMedium, color = cs.onSurface)
            Text(desc, style = MaterialTheme.typography.bodyLarge, color = cs.onSurfaceVariant)
        }
        Box(Modifier.fillMaxWidth().weight(1f, fill = false).padding(start = 20.dp, end = 20.dp, bottom = 24.dp), contentAlignment = Alignment.BottomCenter) {
            visual()
        }
    }
}

// ──────────────────────────────────────────── Linux first ────────────────────────────────────────────

private val linuxLogos = listOf("svg/archlinux.svg", "svg/ubuntu.svg", "svg/fedora.svg", "svg/debian.svg", "svg/opensuse.svg", "svg/nixos-snowflake.svg")
private val otherPlatformLogos = listOf("svg/windows.svg", "svg/MacOS_logo.svg", "svg/android.svg")

@Composable
private fun LinuxSection() {
    val s = site()
    val cs = MaterialTheme.colorScheme
    val navigate = LocalNavigate.current
    val perkIcons = listOf(Icons.Rounded.SettingsRemote, Icons.Rounded.Shield, Icons.Rounded.Inventory2, Icons.Rounded.Verified)
    ContentWidth {
        val wide = maxWidth >= 980.dp
        Column(verticalArrangement = Arrangement.spacedBy(40.dp)) {
            SectionHeading(s.linuxEyebrow, s.linuxCardTitle, s.linuxCardDesc, Modifier.reveal(), eyebrowIcon = Icons.Rounded.Terminal)
            // Expressive grouped list: soft tonal rows, big corners only on the outside of the group.
            val perks: @Composable (Modifier) -> Unit = { m ->
                Column(m, verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    s.linuxPerks.forEachIndexed { i, perk ->
                        val shape = when (i) {
                            0 -> RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp, bottomStart = 6.dp, bottomEnd = 6.dp)
                            s.linuxPerks.lastIndex -> RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp, bottomStart = 28.dp, bottomEnd = 28.dp)
                            else -> RoundedCornerShape(6.dp)
                        }
                        Row(
                            Modifier.fillMaxWidth().clip(shape).background(cs.surfaceContainerLow).padding(horizontal = 20.dp, vertical = 18.dp),
                            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Box(Modifier.size(44.dp).clip(CircleShape).background(cs.secondaryContainer), contentAlignment = Alignment.Center) {
                                Icon(perkIcons[i % perkIcons.size], null, tint = cs.onSecondaryContainer, modifier = Modifier.size(22.dp))
                            }
                            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                Text(perk.title, style = MaterialTheme.typography.titleMedium, color = cs.onSurface)
                                Text(perk.desc, style = MaterialTheme.typography.bodyMedium, color = cs.onSurfaceVariant)
                            }
                        }
                    }
                }
            }
            val side: @Composable (Modifier) -> Unit = { m ->
                Column(m, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Surface(Modifier.fillMaxWidth(), shape = RoundedCornerShape(32.dp), color = cs.surfaceContainerLow) {
                        Column(Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(18.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                                Box(Modifier.size(60.dp).clip(CircleShape).background(cs.surfaceContainerHighest), contentAlignment = Alignment.Center) {
                                    HtmlImage("svg/linux-svgrepo-com.svg", "Linux", 0.dp, MediaFit.Contain, Modifier.size(34.dp))
                                }
                                Column {
                                    Text("Linux", style = MaterialTheme.typography.titleLarge, color = cs.onSurface)
                                    Text("AppImage · .deb · .rpm · AUR · Nix", style = MaterialTheme.typography.bodyMedium, color = cs.onSurfaceVariant)
                                }
                            }
                            Wrap(hSpacing = 8.dp, vSpacing = 8.dp) {
                                linuxLogos.forEach { src ->
                                    Box(Modifier.size(48.dp).clip(CircleShape).background(cs.surfaceContainer), contentAlignment = Alignment.Center) {
                                        HtmlImage(src, src.substringAfterLast('/').substringBefore('.'), 0.dp, MediaFit.Contain, Modifier.size(26.dp))
                                    }
                                }
                            }
                            FilledTonalButton(onClick = { navigate(Page.DOWNLOAD) }, shapes = ButtonDefaults.shapes(), modifier = Modifier.height(48.dp), contentPadding = PaddingValues(horizontal = 22.dp)) {
                                Icon(Icons.Rounded.Download, null, modifier = Modifier.size(20.dp))
                                Spacer(Modifier.width(10.dp))
                                Text(s.linuxCta, style = MaterialTheme.typography.titleSmall)
                            }
                        }
                    }
                    Surface(Modifier.fillMaxWidth(), shape = RoundedCornerShape(32.dp), color = cs.surfaceContainerLow) {
                        Column(Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                otherPlatformLogos.forEach { src ->
                                    Box(Modifier.size(40.dp).clip(CircleShape).background(cs.surfaceContainer), contentAlignment = Alignment.Center) {
                                        HtmlImage(src, src.substringAfterLast('/').substringBefore('.'), 0.dp, MediaFit.Contain, Modifier.size(22.dp))
                                    }
                                }
                            }
                            Text(s.linuxOthersTitle, style = MaterialTheme.typography.titleMedium, color = cs.onSurface)
                            Text(s.linuxOthersDesc, style = MaterialTheme.typography.bodyMedium, color = cs.onSurfaceVariant)
                        }
                    }
                }
            }
            if (wide) {
                Row(horizontalArrangement = Arrangement.spacedBy(20.dp), verticalAlignment = Alignment.Top) {
                    perks(Modifier.weight(1.25f).reveal(0))
                    side(Modifier.weight(1f).reveal(100))
                }
            } else {
                perks(Modifier.fillMaxWidth().reveal())
                side(Modifier.fillMaxWidth().reveal())
            }
        }
    }
}

// ─────────────────────────────────────────── No borders ───────────────────────────────────────────

@Composable
private fun UnblockSection() {
    val s = site()
    val cs = MaterialTheme.colorScheme
    ContentWidth {
        Column(verticalArrangement = Arrangement.spacedBy(40.dp)) {
            SectionHeading(s.blockEyebrow, s.bypassCardTitle, s.bypassCardDesc, Modifier.reveal(), eyebrowIcon = Icons.Rounded.Public)
            Surface(Modifier.fillMaxWidth().reveal(100), shape = RoundedCornerShape(44.dp), color = cs.surfaceContainerLow) {
                ViewportGate(Modifier.padding(28.dp)) { UnblockShowcase(Modifier.fillMaxWidth()) }
            }
        }
    }
}

// ────────────────────────────────────────── Advantages ──────────────────────────────────────────

@Composable
private fun AdvantagesSection() {
    val s = site()
    val cs = MaterialTheme.colorScheme
    val icons = listOf(
        Icons.Rounded.DesktopWindows, Icons.Rounded.CloudSync, Icons.Rounded.HighQuality, Icons.Rounded.Shield,
        Icons.Rounded.Bolt, Icons.Rounded.Devices, Icons.Rounded.Tune, Icons.Rounded.VpnLock,
    )
    val polys = listOf(
        MaterialShapes.Cookie9Sided, MaterialShapes.Sunny, MaterialShapes.Clover4Leaf, MaterialShapes.Puffy,
        MaterialShapes.SoftBurst, MaterialShapes.Gem, MaterialShapes.Flower, MaterialShapes.Cookie12Sided,
    )
    ContentWidth {
        val cols = when { maxWidth >= 1100.dp -> 4; maxWidth >= 640.dp -> 2; else -> 1 }
        Column(verticalArrangement = Arrangement.spacedBy(44.dp)) {
            SectionHeading(s.advantagesEyebrow, s.advantagesTitle, s.advantagesSubtitle, Modifier.reveal(), eyebrowIcon = Icons.Rounded.Star)
            ViewportGate {
                val clock = rememberClock()
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    s.advantages.chunked(cols).forEachIndexed { r, row ->
                        Row(Modifier.fillMaxWidth().height(IntrinsicSize.Min), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                            row.forEachIndexed { c, adv ->
                                val i = r * cols + c
                                val hero = i == 0
                                val bg = if (hero) cs.primary else cs.surfaceContainerLow
                                val ink = if (hero) cs.onPrimary else cs.onSurface
                                val sub = if (hero) cs.onPrimary.copy(alpha = 0.85f) else cs.onSurfaceVariant
                                BentoCard(Modifier.weight(1f).fillMaxHeight().reveal(i * 60), color = bg, shape = RoundedCornerShape(36.dp)) {
                                    Column(Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                                        Box(
                                            Modifier.size(56.dp).spinningPolygon(
                                                polys[i % polys.size],
                                                if (hero) cs.onPrimary else if (i % 2 == 0) cs.primaryContainer else cs.tertiaryContainer,
                                                clock, if (i % 2 == 0) 12f else -12f,
                                            ),
                                            contentAlignment = Alignment.Center,
                                        ) {
                                            // Only the shape turns; the glyph stays upright.
                                            Icon(icons[i % icons.size], null, tint = if (hero) cs.primary else if (i % 2 == 0) cs.onPrimaryContainer else cs.onTertiaryContainer, modifier = Modifier.size(26.dp))
                                        }
                                        Text(adv.title, style = MaterialTheme.typography.titleLarge, color = ink)
                                        Text(adv.desc, style = MaterialTheme.typography.bodyMedium, color = sub)
                                    }
                                }
                            }
                            repeat(cols - row.size) { Spacer(Modifier.weight(1f)) }
                        }
                    }
                }
            }
        }
    }
}

// ──────────────────────────────────────────── Bento ────────────────────────────────────────────

private class Tile(
    val weight: Float,
    val title: String,
    val desc: String,
    val icon: ImageVector,
    val accent: Int = 0,
    val demo: @Composable () -> Unit,
)

private const val BentoRowCount = 5

@Composable
private fun BentoHeading() {
    val s = site()
    ContentWidth {
        SectionHeading(s.bentoEyebrow, s.bentoTitle, s.bentoSubtitle, Modifier.reveal().padding(bottom = 48.dp), eyebrowIcon = Icons.Rounded.Widgets)
    }
}

@Composable
private fun bentoRows(): List<Pair<Dp, List<Tile>>> {
    val s = site()
    return listOf(
        560.dp to listOf(
            Tile(2f, s.lyricsTitle, s.lyricsDesc, Icons.Rounded.Lyrics, 0) { LyricsCardDemo(Modifier.fillMaxWidth().height(380.dp)) },
            Tile(1f, s.fxTitle, s.fxDesc, Icons.Rounded.GraphicEq, 1) { AudioFxDemo(Modifier.fillMaxWidth(), compact = true) },
        ),
        470.dp to listOf(
            Tile(1f, s.shazamTitle, s.shazamDesc, Icons.Rounded.Mic, 2) { ShazamDemo(Modifier.fillMaxWidth(), height = 320.dp) },
            Tile(1f, s.togetherTitle, s.togetherDesc, Icons.Rounded.Groups, 0) { TogetherDemo(Modifier.fillMaxWidth()) },
            Tile(1f, s.colorTitle, s.colorDesc, Icons.Rounded.Palette, 1) { DynamicColorDemo(Modifier.fillMaxWidth()) },
        ),
        470.dp to listOf(
            Tile(1f, s.stemsTitle, s.stemsDesc, Icons.Rounded.CallSplit, 2) { StemsDemo(Modifier.fillMaxWidth()) },
            Tile(2f, s.connectTitle, s.connectDesc, Icons.Rounded.Devices, 0) { ConnectDemo(Modifier.fillMaxWidth()) },
        ),
        540.dp to listOf(
            Tile(1f, s.translateTitle, s.translateDesc, Icons.Rounded.Translate, 1) { LyricsTranslateDemo(Modifier.fillMaxWidth()) },
            Tile(1f, s.waveTitle, s.waveDesc, Icons.Rounded.Waves, 2) { MyWaveDemo(Modifier.fillMaxWidth()) },
            Tile(1f, s.petTitle, s.petDesc, Icons.Rounded.Pets, 0) { PetDemo(Modifier.fillMaxWidth()) },
        ),
        470.dp to listOf(
            Tile(2f, s.offlineTitle, s.offlineDesc, Icons.Rounded.DownloadForOffline, 1) { OfflineDemo(Modifier.fillMaxWidth()) },
            Tile(1f, s.syncTitle, s.syncDesc, Icons.Rounded.CloudSync, 2) { SyncDemo(Modifier.fillMaxWidth()) },
        ),
    )
}

@Composable
private fun BentoRow(index: Int) {
    val (h, tiles) = bentoRows()[index]
    ContentWidth {
        val cols = when { maxWidth >= 1100.dp -> 3; maxWidth >= 720.dp -> 2; else -> 1 }
        Column(Modifier.padding(bottom = 20.dp), verticalArrangement = Arrangement.spacedBy(20.dp)) {
            when (cols) {
                3 -> Row(Modifier.fillMaxWidth().height(h), horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                    tiles.forEachIndexed { i, tile -> BentoTile(tile, Modifier.weight(tile.weight).fillMaxHeight().reveal(i * 90), fixedHeight = true) }
                }
                2 -> tiles.chunked(2).forEach { pair ->
                    Row(Modifier.fillMaxWidth().height(500.dp), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        pair.forEachIndexed { i, tile -> BentoTile(tile, Modifier.weight(1f).fillMaxHeight().reveal(i * 90), fixedHeight = true) }
                        if (pair.size == 1) Spacer(Modifier.weight(1f))
                    }
                }
                else -> tiles.forEach { tile -> BentoTile(tile, Modifier.fillMaxWidth().reveal(), fixedHeight = false) }
            }
        }
    }
}

@Composable
private fun BentoTile(tile: Tile, modifier: Modifier, fixedHeight: Boolean) {
    val cs = MaterialTheme.colorScheme
    val containers = listOf(cs.primaryContainer to cs.onPrimaryContainer, cs.tertiaryContainer to cs.onTertiaryContainer, cs.secondaryContainer to cs.onSecondaryContainer)
    val (c, ink) = containers[tile.accent % 3]
    val polys = listOf(MaterialShapes.Cookie9Sided, MaterialShapes.Sunny, MaterialShapes.Clover4Leaf)
    BentoCard(modifier, color = cs.surfaceContainerLow, shape = RoundedCornerShape(36.dp)) {
        Row(Modifier.padding(start = 24.dp, end = 24.dp, top = 24.dp), horizontalArrangement = Arrangement.spacedBy(14.dp), verticalAlignment = Alignment.Top) {
            ShapeIcon(tile.icon, polys[tile.accent % 3], 44.dp, c, ink)
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(tile.title, style = MaterialTheme.typography.titleLarge, color = cs.onSurface)
                Text(tile.desc, style = MaterialTheme.typography.bodyMedium, color = cs.onSurfaceVariant)
            }
        }
        ViewportGate(
            if (fixedHeight) Modifier.fillMaxWidth().weight(1f).padding(20.dp)
            else Modifier.fillMaxWidth().heightIn(min = 240.dp).padding(20.dp)
        ) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { tile.demo() }
        }
    }
}

// ───────────────────────────────────────── Real screens ─────────────────────────────────────────

@Composable
private fun RealScreensSection() {
    val s = site()
    val cs = MaterialTheme.colorScheme
    var tab by remember { mutableIntStateOf(0) }
    ContentWidth {
        val compact = maxWidth < 720.dp
        Column(verticalArrangement = Arrangement.spacedBy(36.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            SectionHeading(s.screensEyebrow, s.screensTitle, s.screensSubtitle, Modifier.fillMaxWidth().reveal(), centered = true, eyebrowIcon = Icons.Rounded.PhotoLibrary)
            Row(horizontalArrangement = Arrangement.spacedBy(ButtonGroupDefaults.ConnectedSpaceBetween), modifier = Modifier.reveal(100)) {
                listOf(s.tabAndroid to Icons.Rounded.PhoneAndroid, s.tabDesktop to Icons.Rounded.DesktopWindows).forEachIndexed { i, (label, icon) ->
                    ToggleButton(
                        checked = tab == i,
                        onCheckedChange = { tab = i },
                        shapes = if (i == 0) ButtonGroupDefaults.connectedLeadingButtonShapes() else ButtonGroupDefaults.connectedTrailingButtonShapes(),
                        modifier = Modifier.height(52.dp),
                        contentPadding = PaddingValues(horizontal = 24.dp),
                    ) {
                        Icon(icon, null, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(label, style = MaterialTheme.typography.titleSmall)
                    }
                }
            }
            AnimatedContent(tab, transitionSpec = { (fadeIn(tween(300)) + scaleIn(initialScale = 0.96f)) togetherWith fadeOut(tween(150)) }, label = "screens") { t ->
                if (t == 0) AndroidFan(compact) else DesktopGallery()
            }
        }
    }
}

private val androidShots: List<Pair<DrawableResource, String>> by lazy {
    listOf(
        Res.drawable.android_welcomescreen to "Welcome",
        Res.drawable.android_homescreen to "Home",
        Res.drawable.android_playerscreen to "Player",
        Res.drawable.android_lyrics to "Lyrics",
        Res.drawable.android_audiofx to "Audio FX",
        Res.drawable.android_shazam to "Shazam",
        Res.drawable.android_profile to "Profile",
        Res.drawable.android_transferyourgems to "Import",
        Res.drawable.android_iconchanger to "App icons",
        Res.drawable.android_amoled to "AMOLED",
        Res.drawable.android_settings to "Settings",
    )
}

@Composable
private fun AndroidFan(compact: Boolean) {
    val cs = MaterialTheme.colorScheme
    Row(
        Modifier.fillMaxWidth().fadingEdges().horizontalScroll(rememberScrollState()).padding(vertical = 48.dp),
        horizontalArrangement = Arrangement.spacedBy(if (compact) 12.dp else (-28).dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Spacer(Modifier.width(if (compact) 0.dp else 40.dp))
        androidShots.forEachIndexed { i, (res, label) ->
            val source = remember { MutableInteractionSource() }
            val hovered by source.collectIsHoveredAsState()
            val lift by animateFloatAsState(if (hovered) 1f else 0f, spring(0.8f, 380f))
            val baseRot = if (compact) 0f else ((i % 3) - 1) * 3f
            Column(
                Modifier
                    .zIndex(if (hovered) 10f else 0f)
                    .hoverable(source)
                    .graphicsLayer {
                        rotationZ = baseRot * (1f - lift)
                        translationY = -lift * 10.dp.toPx() + (if (i % 2 == 0) 0f else 14.dp.toPx()) * (1f - lift)
                        scaleX = 1f + lift * 0.025f; scaleY = scaleX
                    },
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Screenshot(res, label, Modifier.height(if (compact) 440.dp else 520.dp))
                Text(label, style = MaterialTheme.typography.labelLarge, color = if (hovered) cs.primary else cs.onSurfaceVariant)
            }
        }
        Spacer(Modifier.width(40.dp))
    }
}

@Composable
private fun DesktopGallery() {
    val s = site()
    val cs = MaterialTheme.colorScheme
    val shots = listOf(Res.drawable.desk_home, Res.drawable.desk_player, Res.drawable.desk_lyrics, Res.drawable.desk_playlist)
    var idx by remember { mutableIntStateOf(0) }
    var userPicked by remember { mutableStateOf(false) }
    LaunchedEffect(userPicked) {
        if (userPicked) return@LaunchedEffect
        while (true) { delay(4200); idx = (idx + 1) % shots.size }
    }
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(20.dp), modifier = Modifier.fillMaxWidth()) {
        WindowFrame(Modifier.fillMaxWidth(), title = "KittyTune · ${s.desktopShots[idx]}") {
            AnimatedContent(idx, transitionSpec = { fadeIn(tween(450)) togetherWith fadeOut(tween(450)) }, label = "desk") { i ->
                Screenshot(shots[i], s.desktopShots[i], Modifier.fillMaxWidth().aspectRatio(16f / 9.6f), contentScale = ContentScale.Crop)
            }
        }
        Wrap(hSpacing = 8.dp, vSpacing = 8.dp, center = true) {
            s.desktopShots.forEachIndexed { i, label ->
                ToggleButton(
                    checked = idx == i,
                    onCheckedChange = { idx = i; userPicked = true },
                    modifier = Modifier.height(40.dp),
                    contentPadding = PaddingValues(horizontal = 18.dp),
                ) { Text(label, style = MaterialTheme.typography.labelLarge) }
            }
        }
    }
}

// ─────────────────────────────────────────── Compare ───────────────────────────────────────────

@Composable
private fun CompareSection() {
    val s = site()
    val cs = MaterialTheme.colorScheme
    ContentWidth(maxWidth = 1040.dp) {
        val compact = maxWidth < 640.dp
        Column(verticalArrangement = Arrangement.spacedBy(40.dp)) {
            SectionHeading(s.compareEyebrow, s.compareTitle, null, Modifier.fillMaxWidth().reveal(), centered = true, eyebrowIcon = Icons.Rounded.CompareArrows)
            Surface(Modifier.fillMaxWidth().reveal(120), shape = RoundedCornerShape(40.dp), color = cs.surfaceContainerLow) {
                Column(Modifier.padding(if (compact) 14.dp else 28.dp)) {
                    Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Spacer(Modifier.weight(1.2f))
                        Text(s.compareWeb, style = MaterialTheme.typography.titleSmall, color = cs.onSurfaceVariant, modifier = Modifier.weight(1f), textAlign = TextAlign.Center)
                        Row(Modifier.weight(1f), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
                            KittyLogo(Modifier.size(22.dp))
                            Spacer(Modifier.width(6.dp))
                            Text(s.compareKitty, style = MaterialTheme.typography.titleSmall, color = cs.primary)
                        }
                    }
                    s.compareRows.forEachIndexed { i, row ->
                        Row(
                            Modifier.fillMaxWidth().clip(RoundedCornerShape(24.dp)).background(if (i % 2 == 0) cs.surfaceContainer else Color.Transparent).padding(horizontal = 12.dp, vertical = 14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(row.label, style = if (compact) MaterialTheme.typography.labelLarge else MaterialTheme.typography.titleSmall, color = cs.onSurface, modifier = Modifier.weight(1.2f))
                            Row(Modifier.weight(1f), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
                                if (row.web == "—") {
                                    Icon(Icons.Rounded.Close, null, tint = cs.error.copy(alpha = 0.8f), modifier = Modifier.size(20.dp))
                                } else {
                                    Text(row.web, style = MaterialTheme.typography.bodySmall, color = cs.onSurfaceVariant, textAlign = TextAlign.Center)
                                }
                            }
                            Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
                                Surface(shape = CircleShape, color = cs.primaryContainer) {
                                    Row(Modifier.padding(horizontal = 10.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                        if (!compact) Icon(Icons.Rounded.Check, null, tint = cs.onPrimaryContainer, modifier = Modifier.size(16.dp))
                                        Text(row.kitty, style = MaterialTheme.typography.labelMedium, color = cs.onPrimaryContainer, textAlign = TextAlign.Center)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// ───────────────────────────────────────── Integrations ─────────────────────────────────────────

@Composable
private fun IntegrationsSection() {
    val s = site()
    val cs = MaterialTheme.colorScheme
    val icons = listOf(Icons.Rounded.Forum, Icons.Rounded.Tune, Icons.Rounded.DirectionsCar, Icons.Rounded.Widgets, Icons.Rounded.Keyboard, Icons.Rounded.VpnLock)
    val polys = listOf(MaterialShapes.Cookie9Sided, MaterialShapes.Sunny, MaterialShapes.Clover4Leaf, MaterialShapes.Puffy, MaterialShapes.Gem, MaterialShapes.SoftBurst)
    ContentWidth {
        val wide = maxWidth >= 1000.dp
        val cols = when { maxWidth >= 1000.dp -> 2; maxWidth >= 600.dp -> 2; else -> 1 }
        Column(verticalArrangement = Arrangement.spacedBy(48.dp)) {
            SectionHeading(s.integrationsEyebrow, s.integrationsTitle, null, Modifier.reveal(), eyebrowIcon = Icons.Rounded.Extension)
            val list: @Composable (Modifier) -> Unit = { m ->
                Column(m, verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    s.integrations.chunked(cols).forEachIndexed { r, row ->
                        Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                            row.forEachIndexed { c, item ->
                                val i = r * cols + c
                                BentoCard(Modifier.weight(1f).reveal(i * 60), color = cs.surfaceContainerLow, shape = RoundedCornerShape(28.dp)) {
                                    Row(Modifier.padding(18.dp), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                                        ShapeIcon(icons[i % icons.size], polys[i % polys.size], 44.dp, if (i % 2 == 0) cs.primaryContainer else cs.tertiaryContainer, if (i % 2 == 0) cs.onPrimaryContainer else cs.onTertiaryContainer)
                                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                            Text(item.title, style = MaterialTheme.typography.titleMedium, color = cs.onSurface)
                                            Text(item.desc, style = MaterialTheme.typography.bodyMedium, color = cs.onSurfaceVariant)
                                        }
                                    }
                                }
                            }
                            repeat(cols - row.size) { Spacer(Modifier.weight(1f)) }
                        }
                    }
                }
            }
            val visuals: @Composable (Modifier) -> Unit = { m ->
                ViewportGate(m) {
                    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        DiscordDemo(Modifier.fillMaxWidth().reveal(100))
                        NativePanelsCard(Modifier.fillMaxWidth().reveal(200))
                    }
                }
            }
            if (wide) {
                Row(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                    list(Modifier.weight(1.4f))
                    visuals(Modifier.weight(1f))
                }
            } else {
                list(Modifier.fillMaxWidth())
                visuals(Modifier.fillMaxWidth())
            }
        }
    }
}

/** Native media panels: each OS's own widget, shown whole and centred on a neutral plate. */
@Composable
private fun NativePanelsCard(modifier: Modifier) {
    val s = site()
    val cs = MaterialTheme.colorScheme
    Surface(modifier, shape = RoundedCornerShape(32.dp), color = cs.surfaceContainerLow) {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(s.osShotsTitle, style = MaterialTheme.typography.titleMedium, color = cs.onSurface, modifier = Modifier.padding(start = 4.dp))
            PanelShot(Res.drawable.os_musicpanelmpris, "Hyprland · MPRIS", 586f / 238f, Modifier.fillMaxWidth())
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.Top) {
                PanelShot(Res.drawable.os_kdeplasma, "KDE Plasma", 572f / 575f, Modifier.weight(1f))
                PanelShot(Res.drawable.os_windowsmedia, "Windows 11", 380f / 192f, Modifier.weight(1f), plateRatio = 572f / 575f)
            }
        }
    }
}

@Composable
private fun PanelShot(res: DrawableResource, label: String, ratio: Float, modifier: Modifier, plateRatio: Float = ratio) {
    val cs = MaterialTheme.colorScheme
    Column(modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Box(
            Modifier.fillMaxWidth().aspectRatio(plateRatio).clip(RoundedCornerShape(20.dp)).background(cs.surfaceContainerHighest),
            contentAlignment = Alignment.Center
        ) {
            Screenshot(
                res, label,
                Modifier.fillMaxWidth().aspectRatio(ratio).clip(RoundedCornerShape(if (plateRatio == ratio) 20.dp else 12.dp)),
                contentScale = ContentScale.Fit
            )
        }
        TagPill(label, cs.secondaryContainer, cs.onSecondaryContainer)
    }
}

// ─────────────────────────────────────────── Final CTA ───────────────────────────────────────────

@Composable
private fun FinalCta() {
    val s = site()
    val cs = MaterialTheme.colorScheme
    val navigate = LocalNavigate.current
    ContentWidth {
        val compact = maxWidth < 760.dp
        ViewportGate(Modifier.fillMaxWidth().reveal()) {
            Box(Modifier.fillMaxWidth().clip(RoundedCornerShape(if (compact) 40.dp else 56.dp)).background(cs.primary)) {
                MorphingBlob(Modifier.size(420.dp).align(Alignment.TopEnd).offset(x = 140.dp, y = (-140).dp), color = cs.onPrimary.copy(alpha = 0.18f), spinDegPerSecond = 10f)
                MorphingBlob(Modifier.size(260.dp).align(Alignment.BottomStart).offset(x = (-90).dp, y = 90.dp), color = cs.onPrimary.copy(alpha = 0.14f), phase = 0.4f, spinDegPerSecond = -12f)
                val content: @Composable (Modifier) -> Unit = { m ->
                    Column(m, verticalArrangement = Arrangement.spacedBy(20.dp)) {
                        Text(s.ctaTitle, style = if (compact) MaterialTheme.typography.displaySmall else MaterialTheme.typography.displayMedium, color = cs.onPrimary)
                        Text(s.ctaSubtitle, style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.W500), color = cs.onPrimary.copy(alpha = 0.85f))
                        Wrap(hSpacing = 12.dp, vSpacing = 12.dp) {
                            Button(
                                onClick = { navigate(Page.DOWNLOAD) },
                                shapes = ButtonDefaults.shapes(),
                                colors = ButtonDefaults.buttonColors(containerColor = cs.onPrimary, contentColor = cs.primary),
                                contentPadding = PaddingValues(horizontal = 28.dp),
                                modifier = Modifier.height(60.dp)
                            ) {
                                Icon(Icons.Rounded.Download, null)
                                Spacer(Modifier.width(10.dp))
                                Text(s.ctaDownload, style = MaterialTheme.typography.titleMedium)
                            }
                            OutlinedButton(
                                onClick = { openUrl(KofiUrl) },
                                shapes = ButtonDefaults.shapes(),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = cs.onPrimary),
                                border = androidx.compose.foundation.BorderStroke(1.5.dp, cs.onPrimary.copy(alpha = 0.6f)),
                                contentPadding = PaddingValues(horizontal = 24.dp),
                                modifier = Modifier.height(60.dp)
                            ) {
                                Icon(Icons.Rounded.Coffee, null)
                                Spacer(Modifier.width(10.dp))
                                Text(s.ctaKofi, style = MaterialTheme.typography.titleMedium)
                            }
                        }
                    }
                }
                if (compact) {
                    Column(Modifier.padding(28.dp), verticalArrangement = Arrangement.spacedBy(24.dp)) {
                        PixelPet(PetMood.Dj, Modifier.fillMaxWidth(0.8f).align(Alignment.CenterHorizontally))
                        content(Modifier.fillMaxWidth())
                    }
                } else {
                    Row(Modifier.padding(56.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(40.dp)) {
                        content(Modifier.weight(1.3f))
                        PixelPet(PetMood.Dj, Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

// ──────────────────────────────────────────── Footer ────────────────────────────────────────────

@Composable
fun SiteFooter() {
    val s = site()
    val chrome = appChromeStrings(LocalWebLocale.current)
    val cs = MaterialTheme.colorScheme
    val navigate = LocalNavigate.current
    Surface(Modifier.fillMaxWidth(), color = cs.surfaceContainerLow, shape = RoundedCornerShape(topStart = 48.dp, topEnd = 48.dp)) {
        ContentWidth {
            val compact = maxWidth < 760.dp
            Column(Modifier.padding(vertical = 56.dp), verticalArrangement = Arrangement.spacedBy(40.dp)) {
                val brand: @Composable (Modifier) -> Unit = { m ->
                    Column(m, verticalArrangement = Arrangement.spacedBy(14.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            Box(Modifier.size(48.dp).clip(PolygonShape(MaterialShapes.Cookie9Sided)).background(cs.primary), contentAlignment = Alignment.Center) {
                                KittyLogo(Modifier.size(30.dp), tint = cs.onPrimary)
                            }
                            Text("KittyTune", style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.W900), color = cs.onSurface)
                        }
                        Text(s.footerTagline, style = MaterialTheme.typography.bodyLarge, color = cs.onSurfaceVariant, modifier = Modifier.widthIn(max = 360.dp))
                    }
                }
                val col: @Composable (String, List<Pair<String, () -> Unit>>, Modifier) -> Unit = { title, links, m ->
                    Column(m, verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(title, style = MaterialTheme.typography.titleSmall, color = cs.onSurface)
                        links.forEach { (label, action) ->
                            Text(
                                label, style = MaterialTheme.typography.bodyMedium, color = cs.onSurfaceVariant,
                                modifier = Modifier.clip(RoundedCornerShape(8.dp)).clickable(onClick = action).padding(vertical = 2.dp)
                            )
                        }
                    }
                }
                val product = listOf(
                    chrome.navHome to { navigate(Page.HOME) },
                    chrome.navFeatures to { navigate(Page.FEATURES) },
                    chrome.navTogether to { navigate(Page.TOGETHER) },
                    chrome.navDownload to { navigate(Page.DOWNLOAD) },
                )
                val source = listOf(
                    s.footerAndroidRepo to { openUrl(RepoAndroid) },
                    s.footerDesktopRepo to { openUrl(RepoDesktop) },
                    s.footerWebsiteRepo to { openUrl(RepoWebsite) },
                    s.footerReleases to { openUrl("$RepoAndroid/releases") },
                )
                val community = listOf(
                    s.footerIssues to { openUrl("$RepoAndroid/issues") },
                    "Ko-fi" to { openUrl(KofiUrl) },
                )
                if (compact) {
                    brand(Modifier.fillMaxWidth())
                    Row(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                        col(s.footerProduct, product, Modifier.weight(1f))
                        col(s.footerSource, source, Modifier.weight(1f))
                    }
                    col(s.footerCommunity, community, Modifier.fillMaxWidth())
                } else {
                    Row(horizontalArrangement = Arrangement.spacedBy(32.dp)) {
                        brand(Modifier.weight(1.6f))
                        col(s.footerProduct, product, Modifier.weight(1f))
                        col(s.footerSource, source, Modifier.weight(1f))
                        col(s.footerCommunity, community, Modifier.weight(1f))
                    }
                }
                HorizontalDivider(color = cs.outlineVariant.copy(alpha = 0.5f))
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text("© 2026 alan7383 · MIT", style = MaterialTheme.typography.bodySmall, color = cs.onSurfaceVariant, modifier = Modifier.weight(1f))
                    Text(s.footerMade, style = MaterialTheme.typography.bodySmall, color = cs.onSurfaceVariant, textAlign = TextAlign.End, modifier = Modifier.weight(1f))
                }
            }
        }
    }
}
