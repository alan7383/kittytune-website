package com.alananasss.kittytunewebsite.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.DesktopWindows
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.browser.window
import kotlinx.coroutines.delay

private data class PlatformSummary(
    val name: String,
    val label: String,
    val icon: ImageVector,
    val description: String,
    val points: List<String>,
)

private data class HomeCapability(
    val title: String,
    val description: String,
    val icon: ImageVector,
)

private val platformSummaries = listOf(
    PlatformSummary(
        name = "Android",
        label = "Mobile APK",
        icon = Icons.Filled.PhoneAndroid,
        description = "The original Android app stays true to Pixel: Material You, widgets, lyrics, SoundCloud, and YouTube in a touch-friendly interface.",
        points = listOf("Dynamic Colors", "Android Widgets", "Audio FX", "Synced Lyrics")
    ),
    PlatformSummary(
        name = "Desktop",
        label = "Windows, macOS, Linux",
        icon = Icons.Filled.DesktopWindows,
        description = "The desktop version uses the same Compose foundation, but the interface breathes on large screens with a library, playlists, and system integrations.",
        points = listOf("SMTC and MPRIS", "Local Files", "Large Libraries", "Keyboard Controls")
    ),
)

private val homeCapabilities = listOf(
    HomeCapability(
        title = "SoundCloud Connection",
        description = "Official two-way synchronization of your likes, playlists, reposts, and listening history.",
        icon = Icons.Filled.CloudSync
    ),
    HomeCapability(
        title = "Hybrid Audio",
        description = "SoundCloud, YouTube, and local files coexisting without ads or restrictive algorithms.",
        icon = Icons.Filled.LibraryMusic
    ),
    HomeCapability(
        title = "Real-time Effects",
        description = "Nightcore, 8D Audio, bass boost, and pitch adjustments without latency while listening.",
        icon = Icons.Filled.GraphicEq
    ),
    HomeCapability(
        title = "Local-first",
        description = "No telemetry. Download caching and UI preferences remain entirely under your control.",
        icon = Icons.Filled.Security
    ),
)

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun LoadingScreen() {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier.fillMaxSize(),
    ) {
        ContainedLoadingIndicator(modifier = Modifier.size(128.dp))
    }
}

@Composable
fun HomePage(onDownloadClick: () -> Unit) {
    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val isMobile = maxWidth < 600.dp
        DesktopScrollableColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(if (isMobile) 80.dp else 160.dp)
        ) {
            HeroSection(onDownloadClick)
        StatsRow()
        PlatformOverview()
        SharedCapabilities()

        FeatureShowcaseRow(
            title = "Android keeps its Pixel identity.",
            description = "The Astro page can disappear: the Kotlin site now presents the APK with Dynamic Colors, widgets, lyrics, and audio FX in the same visual language as the desktop.",
            asset = MediaAsset(
                fileName = "android-home.png",
                title = "KittyTune Android",
                caption = "A Material You interface designed for Android 12+ and usable from Android 8.0.",
                fit = MediaFit.Contain
            ),
            textLeft = true
        )

        FeatureShowcaseRow(
            title = "Desktop is not just a scaled-up version.",
            description = "On Windows, macOS, and Linux, KittyTune takes advantage of space: wide library, comfortable lists, persistent playback, and native media integrations.",
            asset = MediaAsset(
                fileName = "libraryfullscreen.mp4",
                title = "Desktop Library",
                caption = "Wide, readable views adapted for long sessions."
            ),
            textLeft = false
        )

        FeatureShowcaseRow(
            title = "The sound remains in your hands.",
            description = "Pitch, speed, 8D, bass boost, or muffled mode: effects are made to be understood quickly, then forgotten while listening.",
            asset = MediaAsset(
                fileName = "effects.mp4",
                title = "Audio FX",
                caption = "Direct controls, without complicated panels."
            ),
            textLeft = true
        )

        FooterSection()
        Spacer(Modifier.height(32.dp))
        }
    }
}

@Composable
fun FeatureShowcaseRow(
    title: String,
    description: String,
    asset: MediaAsset,
    textLeft: Boolean
) {
    Surface(
        modifier = Modifier.fillMaxWidth().responsivePagePadding(),
        color = Color.Transparent
    ) {
        val textContent: @Composable (Modifier) -> Unit = { childModifier ->
            SectionHeader(
                title = title,
                description = description,
                modifier = childModifier
            )
        }
        val mediaContent: @Composable (Modifier) -> Unit = { childModifier ->
            MediaCard(
                asset = asset,
                aspectRatio = if (asset.isVideo) 16f / 10f else 16f / 9f,
                modifier = childModifier
            )
        }

        AdaptivePair(
            spacing = 72.dp,
            first = if (textLeft) textContent else mediaContent,
            second = if (textLeft) mediaContent else textContent
        )
    }
}

@Composable
fun HeroSection(onDownloadClick: () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = Color.Transparent
    ) {
        BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
            val compact = maxWidth < 700.dp
            val horizontalPadding = if (compact) 24.dp else 48.dp
            val verticalPadding = if (compact) 44.dp else 76.dp

            AdaptivePair(
                modifier = Modifier.padding(horizontal = horizontalPadding, vertical = verticalPadding),
                spacing = 72.dp,
                first = { childModifier ->
                    Column(
                        modifier = childModifier,
                        verticalArrangement = Arrangement.spacedBy(if (compact) 20.dp else 26.dp)
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.secondaryContainer,
                        ) {
                            Text(
                                text = "Android + Desktop - Compose Multiplatform",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSecondaryContainer,
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                            )
                        }

                        Text(
                            "KittyTune",
                            style = if (compact) MaterialTheme.typography.displayMedium else MaterialTheme.typography.displayLarge,
                            fontWeight = FontWeight.Black,
                            color = MaterialTheme.colorScheme.onSurface,
                            lineHeight = if (compact) 56.sp else 72.sp
                        )

                        Text(
                            "An open-source player for Android and desktop: SoundCloud, YouTube, local files, synced lyrics, and audio FX. Simple, spacious, without ads.",
                            style = if (compact) MaterialTheme.typography.bodyLarge else MaterialTheme.typography.headlineSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            lineHeight = if (compact) 28.sp else 36.sp
                        )

                        HeroActions(onDownloadClick)
                    }
                },
                second = { childModifier ->
                    MediaCard(
                        asset = MediaAsset(
                            fileName = "herodemo.mp4",
                            title = "Unified Interface",
                            caption = "Designed for large screens while keeping the Android Material You spirit."
                        ),
                        corner = 36.dp,
                        aspectRatio = 16f / 10f,
                        modifier = childModifier
                    )
                }
            )
        }
    }
}

@Composable
private fun HeroActions(onDownloadClick: () -> Unit) {
    BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
        val compact = maxWidth < 520.dp
        val buttonModifier = if (compact) Modifier.fillMaxWidth().height(56.dp) else Modifier.height(56.dp)

        if (compact) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                PrimaryDownloadButton(onDownloadClick, buttonModifier)
                SourceButton(buttonModifier)
            }
        } else {
            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(top = 8.dp)
            ) {
                PrimaryDownloadButton(onDownloadClick, buttonModifier)
                SourceButton(buttonModifier)
            }
        }
    }
}

@Composable
private fun PrimaryDownloadButton(onDownloadClick: () -> Unit, modifier: Modifier) {
    Button(
        onClick = onDownloadClick,
        modifier = modifier,
        shapes = ButtonDefaults.shapes(shape = RoundedCornerShape(50)),
        contentPadding = PaddingValues(horizontal = 28.dp)
    ) {
        Icon(Icons.Filled.Download, contentDescription = null, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(12.dp))
        Text("Download", fontWeight = FontWeight.Bold, fontSize = 16.sp)
    }
}

@Composable
private fun SourceButton(modifier: Modifier) {
    OutlinedButton(
        onClick = { window.open("https://github.com/alan7383/kittytune", "_blank") },
        modifier = modifier,
        shapes = ButtonDefaults.shapes(shape = RoundedCornerShape(50)),
        contentPadding = PaddingValues(horizontal = 28.dp),
    ) {
        Icon(Icons.Filled.Code, contentDescription = null, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(12.dp))
        Text("Source code", fontWeight = FontWeight.Bold, fontSize = 16.sp)
    }
}

@Composable
private fun PlatformOverview() {
    Surface(
        modifier = Modifier.fillMaxWidth().responsivePagePadding(),
        color = Color.Transparent
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(32.dp)) {
            SectionHeader(
                title = "Two platforms, one unified player.",
                description = "Android remains the mobile Material You experience. Desktop becomes the comfortable workspace to manage and listen to all your music."
            )
            BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
                if (maxWidth < 860.dp) {
                    Column(verticalArrangement = Arrangement.spacedBy(20.dp)) {
                        platformSummaries.forEach { summary ->
                            PlatformSummaryCard(summary, Modifier.fillMaxWidth())
                        }
                    }
                } else {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(24.dp)
                    ) {
                        platformSummaries.forEach { summary ->
                            PlatformSummaryCard(summary, Modifier.weight(1f))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PlatformSummaryCard(summary: PlatformSummary, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(32.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow
    ) {
        Column(
            modifier = Modifier.padding(28.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(22.dp),
                    color = MaterialTheme.colorScheme.primaryContainer
                ) {
                    Icon(
                        summary.icon,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.padding(12.dp).size(28.dp)
                    )
                }
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        summary.name,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        summary.label,
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Text(
                summary.description,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                lineHeight = 28.sp
            )

            TagRow(summary.points)
        }
    }
}

@Composable
private fun TagRow(tags: List<String>) {
    BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
        if (maxWidth < 440.dp) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                tags.forEach { tag -> PlatformTag(tag, Modifier.fillMaxWidth()) }
            }
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                tags.chunked(2).forEach { row ->
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        row.forEach { tag -> PlatformTag(tag) }
                    }
                }
            }
        }
    }
}

@Composable
private fun PlatformTag(text: String, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        shape = CircleShape,
        color = MaterialTheme.colorScheme.surfaceContainerHighest
    ) {
        Text(
            text,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
        )
    }
}

@Composable
private fun SharedCapabilities() {
    Surface(
        modifier = Modifier.fillMaxWidth().responsivePagePadding(),
        shape = RoundedCornerShape(32.dp),
        color = MaterialTheme.colorScheme.surfaceContainer
    ) {
        BoxWithConstraints(modifier = Modifier.fillMaxWidth().padding(32.dp)) {
            if (maxWidth < 1000.dp) {
                Column(verticalArrangement = Arrangement.spacedBy(18.dp)) {
                    homeCapabilities.forEach { CapabilityItem(it, Modifier.fillMaxWidth()) }
                }
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(22.dp)) {
                    homeCapabilities.chunked(2).forEach { row ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(22.dp)
                        ) {
                            row.forEach { CapabilityItem(it, Modifier.weight(1f)) }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CapabilityItem(capability: HomeCapability, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Surface(
            modifier = Modifier.size(48.dp),
            shape = RoundedCornerShape(18.dp),
            color = MaterialTheme.colorScheme.secondaryContainer
        ) {
            Icon(
                capability.icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSecondaryContainer,
                modifier = Modifier.padding(12.dp)
            )
        }
        Text(
            capability.title,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Black,
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            capability.description,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            lineHeight = 24.sp
        )
    }
}

@Composable
fun StatsRow() {
    val stats = listOf(
        "Open Source" to "MIT License",
        "Multi-Source" to "SoundCloud, YouTube, Local",
        "Multi-Platform" to "Android, Windows, macOS",
        "Zero Telemetry" to "Local-first design",
    )

    BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
        val isMobile = maxWidth < 600.dp
        val horizontalPad = if (isMobile) 16.dp else 48.dp

        Surface(
            modifier = Modifier.fillMaxWidth().padding(horizontal = horizontalPad),
            shape = RoundedCornerShape(40.dp),
            color = MaterialTheme.colorScheme.surfaceContainerLow
        ) {
            if (isMobile) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 32.dp, horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    stats.forEach { stat ->
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                stat.first,
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Black,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Spacer(Modifier.height(4.dp))
                            Text(
                                stat.second,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 48.dp, horizontal = 48.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    stats.forEach { stat ->
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                stat.first,
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Black,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Spacer(Modifier.height(8.dp))
                            Text(
                                stat.second,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun FooterSection() {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surfaceContainer
    ) {
        BoxWithConstraints(modifier = Modifier.fillMaxWidth().padding(horizontal = 48.dp, vertical = 42.dp)) {
            if (maxWidth < 620.dp) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(18.dp),
                    horizontalAlignment = Alignment.Start
                ) {
                    FooterBrand()
                    FooterLegal()
                }
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    FooterBrand()
                    FooterLegal()
                }
            }
        }
    }
}

@Composable
private fun FooterBrand() {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Icon(
            Icons.Filled.MusicNote,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(32.dp)
        )
        Text(
            "KittyTune",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Black,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
private fun FooterLegal() {
    Text(
        "2026 alan7383 - Open source music player",
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
}

@Composable
fun Modifier.responsivePagePadding(): Modifier {
    return this.then(
        Modifier.padding(horizontal = 16.dp)
    )
}
