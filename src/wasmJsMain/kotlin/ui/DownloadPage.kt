@file:OptIn(kotlin.js.ExperimentalWasmJsInterop::class)
package com.alananasss.kittytunewebsite.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

private data class PlatformInfo(
    val name: String,
    val svgUrl: String? = null,
    val iconVector: ImageVector? = null,
    val packageLabel: String,
    val repo: String,
    val downloadOptions: List<Pair<String, String>>,
    val details: String,
    val cssTransform: String = "",
)

private val platforms = listOf(
    PlatformInfo(
        name = "Windows",
        svgUrl = "svg/windows.svg",
        packageLabel = "MSI",
        repo = "alan7383/KittyTuneDesktop",
        downloadOptions = listOf(
            "Installer (.msi)" to ".msi",
            "Portable (.zip)" to ".zip"
        ),
        details = "For Windows 10, 11 64-bit"
    ),
    PlatformInfo(
        name = "Linux",
        svgUrl = "svg/linux-svgrepo-com.svg",
        packageLabel = "AppImage",
        repo = "alan7383/KittyTuneDesktop",
        downloadOptions = listOf(
            "AppImage (Portable)" to ".AppImage",
            "Debian / Ubuntu (.deb)" to ".deb",
            "Fedora / openSUSE (.rpm)" to ".rpm",
            "Arch Linux (.pkg.tar.zst)" to ".pkg.tar.zst"
        ),
        details = "Portable binary for Linux (x86_64)"
    ),
    PlatformInfo(
        name = "Android",
        svgUrl = "svg/android.svg",
        packageLabel = "APK",
        repo = "alan7383/kittytune",
        downloadOptions = listOf("Direct APK" to ".apk"),
        details = "For Android 8.0+"
    ),
    PlatformInfo(
        name = "macOS",
        svgUrl = "svg/finder-svgrepo-com.svg",
        packageLabel = "DMG",
        repo = "alan7383/KittyTuneDesktop",
        downloadOptions = listOf("Apple Silicon / Intel (.dmg)" to ".dmg"),
        details = "For macOS Apple Silicon & Intel"
    ),
    PlatformInfo(
        name = "GitHub",
        iconVector = Icons.Filled.Code,
        packageLabel = "Source",
        repo = "alan7383/KittyTuneDesktop",
        downloadOptions = emptyList(),
        details = "Source code and releases on GitHub"
    )
)

private data class ReleaseInfo(
    val channel: String,
    val tag: String,
    val date: String,
    val body: String
)

@Composable
fun DownloadPage() {
    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val isMobile = maxWidth < 800.dp
        val horizontalPad = if (isMobile) 16.dp else 48.dp

        DesktopScrollableColumn(
            modifier = Modifier.fillMaxSize(),
            columnModifier = Modifier
                .padding(horizontal = horizontalPad)
                .padding(top = if (isMobile) 32.dp else 72.dp, bottom = 112.dp),
            horizontalAlignment = Alignment.Start,
            verticalArrangement = Arrangement.spacedBy(if (isMobile) 32.dp else 46.dp)
        ) {
            SectionHeader(
                title = "Download KittyTune",
                description = "Android APK and desktop installers in one place. Choose your OS, and get the latest version directly from GitHub."
            )

            if (isMobile) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(24.dp)
                ) {
                    AmneziaStyleDownloadSelector(Modifier.fillMaxWidth())
                    KofiBanner(Modifier.fillMaxWidth())
                }
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth().height(295.dp),
                    horizontalArrangement = Arrangement.spacedBy(32.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(modifier = Modifier.weight(2f).fillMaxHeight()) {
                        AmneziaStyleDownloadSelector(Modifier.fillMaxHeight())
                    }
                    Box(modifier = Modifier.weight(1f).fillMaxHeight()) {
                        KofiBanner(Modifier.fillMaxHeight())
                    }
                }
            }
            Changelog()
        }
    }
}

@Composable
private fun AmneziaStyleDownloadSelector(modifier: Modifier = Modifier) {
    var selectedPlatform by remember { mutableStateOf(platforms.first()) }

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(32.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow
    ) {
        Column(
            modifier = Modifier.padding(vertical = 18.dp, horizontal = 28.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // OS selection row
            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                platforms.forEach { platform ->
                    val isSelected = platform == selectedPlatform
                    val borderColor = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
                    val bgColor = if (isSelected) MaterialTheme.colorScheme.surfaceContainerHighest else Color.Transparent

                    Surface(
                        shape = RoundedCornerShape(24.dp),
                        color = bgColor,
                        border = BorderStroke(if (isSelected) 2.dp else 1.dp, borderColor),
                        modifier = Modifier
                            .size(width = 110.dp, height = 106.dp)
                            .clip(RoundedCornerShape(24.dp))
                            .clickable { selectedPlatform = platform }
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center,
                            modifier = Modifier.fillMaxSize()
                        ) {
                            if (platform.svgUrl != null) {
                                Box(modifier = Modifier.size(36.dp)) {
                                    HtmlImage(
                                        src = platform.svgUrl,
                                        description = platform.name,
                                        corner = 0.dp,
                                        fit = MediaFit.Contain,
                                        cssTransform = platform.cssTransform,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                    Spacer(Modifier.fillMaxSize().background(Color.Transparent))
                                }
                            } else if (platform.iconVector != null) {
                                Icon(
                                    imageVector = platform.iconVector,
                                    contentDescription = platform.name,
                                    modifier = Modifier.size(36.dp),
                                    tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Spacer(Modifier.height(12.dp))
                            Text(
                                text = platform.name,
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = if (isSelected) FontWeight.Black else FontWeight.Medium,
                                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

            // Action area
            BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
                val isMobile = maxWidth < 600.dp
                val isGithub = selectedPlatform.name == "GitHub"

                if (isMobile) {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        DownloadButton(selectedPlatform, isGithub)
                        AlternativeDownloads(selectedPlatform)
                        Text(
                            text = selectedPlatform.details,
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                    }
                } else {
                    // Desktop Mode: We put the other versions BELOW, so they have full width
                    Column(
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                        horizontalAlignment = Alignment.Start
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(24.dp)
                        ) {
                            DownloadButton(selectedPlatform, isGithub)
                            Text(
                                text = selectedPlatform.details,
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        AlternativeDownloads(selectedPlatform)
                    }
                }
            }
        }
    }
}

@Composable
private fun DownloadButton(platform: PlatformInfo, isGithub: Boolean) {
    Button(
        onClick = {
            if (isGithub) {
                openUrlJs("https://github.com/${platform.repo}")
            } else {
                downloadLatestReleaseJs(platform.repo, platform.downloadOptions.first().second)
            }
        },
        shapes = ButtonDefaults.shapes(),
        contentPadding = PaddingValues(horizontal = 32.dp, vertical = 16.dp),
        modifier = Modifier.height(58.dp)
    ) {
        Icon(
            imageVector = if (isGithub) Icons.Default.Code else Icons.Default.Download,
            contentDescription = null,
            modifier = Modifier.size(22.dp)
        )
        Spacer(Modifier.width(12.dp))
        Text(
            text = if (isGithub) "View repository" else "Download ${platform.packageLabel}",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun AlternativeDownloads(platform: PlatformInfo) {
    if (platform.downloadOptions.size > 1) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                "Other versions:",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.horizontalScroll(rememberScrollState())
            ) {
                platform.downloadOptions.drop(1).forEach { (label, ext) ->
                    TextButton(
                        onClick = { downloadLatestReleaseJs(platform.repo, ext) },
                        shapes = ButtonDefaults.shapes()
                    ) {
                        Text(label)
                    }
                }
            }
        }
    }
}

@Composable
private fun KofiBanner(modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(32.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(36.dp)
        ) {
            Text(
                text = "Support KittyTune \u2615",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Black,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(16.dp))
            Text(
                text = "This project is entirely free and open-source. If you enjoy my work, please consider supporting its development by buying me a coffee on Ko-fi!",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                lineHeight = 24.sp,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(24.dp))
            Button(
                onClick = { openUrlJs("https://ko-fi.com/alan7383") },
                shapes = ButtonDefaults.shapes(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                ),
                contentPadding = PaddingValues(horizontal = 24.dp, vertical = 12.dp)
            ) {
                Icon(Icons.Filled.Favorite, contentDescription = null, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(10.dp))
                Text("Ko-fi", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun Changelog() {
    var androidRelease by remember { mutableStateOf<ReleaseInfo?>(null) }
    var desktopRelease by remember { mutableStateOf<ReleaseInfo?>(null) }

    LaunchedEffect(Unit) {
        fetchGithubReleaseJs("alan7383/kittytune") { tag, date, body ->
            val shortDate = if (date.contains("T")) date.substringBefore("T") else date
            androidRelease = ReleaseInfo("Android APK", tag, shortDate, body)
        }
        fetchGithubReleaseJs("alan7383/KittyTuneDesktop") { tag, date, body ->
            val shortDate = if (date.contains("T")) date.substringBefore("T") else date
            desktopRelease = ReleaseInfo("Desktop", tag, shortDate, body)
        }
    }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(28.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow
    ) {
        Column(
            modifier = Modifier.padding(32.dp),
            verticalArrangement = Arrangement.spacedBy(30.dp)
        ) {
            Text(
                "Latest releases",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Black,
                color = MaterialTheme.colorScheme.onSurface
            )

            if (androidRelease == null && desktopRelease == null) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp))
                    Text("Loading release notes...", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            } else {
                androidRelease?.let { ReleaseItem(it) }
                if (androidRelease != null && desktopRelease != null) {
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                }
                desktopRelease?.let { ReleaseItem(it) }
            }
        }
    }
}

@Composable
private fun ReleaseItem(release: ReleaseInfo) {
    BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
        val versionInfo = @Composable { modifier: Modifier ->
            Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    release.channel,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Black,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    "${release.tag} • ${release.date}",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        
        val bodyContent = @Composable { modifier: Modifier ->
            Text(
                text = release.body,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
                lineHeight = 22.sp,
                modifier = modifier
            )
        }

        if (maxWidth < 620.dp) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                versionInfo(Modifier.fillMaxWidth())
                bodyContent(Modifier.fillMaxWidth())
            }
        } else {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(32.dp)
            ) {
                versionInfo(Modifier.width(168.dp))
                Box(modifier = Modifier.weight(1f).heightIn(max = 400.dp).verticalScroll(rememberScrollState())) {
                    bodyContent(Modifier.fillMaxWidth())
                }
            }
        }
    }
}

private fun openUrlJs(url: String) {
    js("window.open(url, '_blank');")
}

private fun fetchGithubReleaseJs(repo: String, onSuccess: (String, String, String) -> Unit) {
    js("""
        fetch('https://api.github.com/repos/' + repo + '/releases/latest')
            .then(res => res.json())
            .then(data => {
                if (data.tag_name) {
                    let body = data.body || '';
                    body = body.replace(/###?/g, '').replace(/\*\*/g, '').trim();
                    onSuccess(data.tag_name, data.published_at || '', body);
                }
            })
            .catch(e => console.error(e));
    """)
}

private fun downloadLatestReleaseJs(repo: String, ext: String) {
    js("""
        fetch('https://api.github.com/repos/' + repo + '/releases/latest')
            .then(res => res.json())
            .then(data => {
                let url = null;
                if (data.assets && data.assets.length > 0) {
                    const asset = data.assets.find(a => a.name.endsWith(ext));
                    if (asset) {
                        url = asset.browser_download_url;
                    }
                }
                if (url) {
                    const a = document.createElement('a');
                    a.href = url;
                    a.download = ''; 
                    document.body.appendChild(a);
                    a.click();
                    document.body.removeChild(a);
                } else {
                    window.open('https://github.com/' + repo + '/releases/latest', '_blank');
                }
            })
            .catch(e => {
                window.open('https://github.com/' + repo + '/releases/latest', '_blank');
            });
    """)
}
