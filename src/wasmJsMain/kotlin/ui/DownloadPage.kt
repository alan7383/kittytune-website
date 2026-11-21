@file:OptIn(kotlin.js.ExperimentalWasmJsInterop::class, androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class)
package com.alananasss.kittytunewebsite.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.border
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Lightbulb
import androidx.compose.material.icons.rounded.FolderOpen
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
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.AlertDialog
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonGroupDefaults
import com.alananasss.kittytunewebsite.ui.kit.springyPress
import androidx.compose.material3.OutlinedButton
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SplitButtonDefaults
import androidx.compose.material3.SplitButtonLayout
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.graphicsLayer
import com.alananasss.kittytunewebsite.ui.kit.reveal
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontFamily
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

private val platformOrder = listOf("Windows", "Linux", "Android", "macOS", "GitHub")

private fun buildPlatforms(s: DownloadStrings): List<PlatformInfo> = listOf(
    PlatformInfo(
        name = "Windows",
        svgUrl = "svg/windows.svg",
        packageLabel = "MSI",
        repo = "alan7383/KittyTuneDesktop",
        downloadOptions = listOf(
            s.windowsInstallerLabel to ".msi",
            s.windowsPortableLabel to ".zip"
        ),
        details = s.windowsDetails
    ),
    PlatformInfo(
        name = "Linux",
        svgUrl = "svg/linux-svgrepo-com.svg",
        packageLabel = "AppImage",
        repo = "alan7383/KittyTuneDesktop",
        downloadOptions = listOf(
            s.linuxOptAppImage to ".AppImage",
            s.linuxOptDeb to ".deb",
            s.linuxOptRpm to ".rpm",
            s.linuxOptArch to ".pkg.tar.zst",
            s.linuxOptNix to "nixos"
        ),
        details = s.linuxDetails
    ),
    PlatformInfo(
        name = "Android",
        svgUrl = "svg/android.svg",
        packageLabel = "APK",
        repo = "alan7383/kittytune",
        // (label, asset name contains) — primary first
        downloadOptions = listOf(
            s.androidArm64Label to "arm64-v8a",
            s.androidArm32Label to "armeabi-v7a",
            s.androidUniversalLabel to "universal",
            "x86_64"                  to "x86_64"
        ),
        details = s.androidDetails
    ),
    PlatformInfo(
        name = "macOS",
        svgUrl = "svg/finder-svgrepo-com.svg",
        packageLabel = "DMG",
        repo = "alan7383/KittyTuneDesktop",
        downloadOptions = listOf(
            s.macArmLabel to "-arm64.dmg",
            s.macIntelLabel to "-x64.dmg"
        ),
        details = s.macDetails
    ),
    PlatformInfo(
        name = "GitHub",
        iconVector = Icons.Filled.Code,
        packageLabel = s.gitHubPackageLabel,
        repo = "alan7383/KittyTuneDesktop",
        downloadOptions = emptyList(),
        details = s.gitHubDetails
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
    val locale = LocalWebLocale.current
    val s = remember(locale) { downloadStrings(locale) }
    val sortedPlatformNames = remember {
        var osName = "Windows"
        getOsJs { osName = it }
        val detected = platformOrder.find { it == osName } ?: platformOrder.first()
        val others = platformOrder.filter { it != detected }
        listOf(detected) + others
    }
    val allPlatforms = remember(s) { buildPlatforms(s) }
    val sortedPlatforms = remember(allPlatforms, sortedPlatformNames) {
        sortedPlatformNames.mapNotNull { name -> allPlatforms.find { it.name == name } }
    }
    var selectedPlatformName by remember { mutableStateOf(sortedPlatformNames.first()) }
    val selectedPlatform = sortedPlatforms.find { it.name == selectedPlatformName } ?: sortedPlatforms.first()

    val linuxDistros = remember(s) { buildLinuxDistros(s) }
    var distroIdForHelp by remember { mutableStateOf<String?>(null) }
    val distroForHelp = remember(linuxDistros, distroIdForHelp) {
        distroIdForHelp?.let { id -> linuxDistros.find { it.id == id } }
    }

    CompositionLocalProvider(LocalDialogActive provides (distroForHelp != null)) {
        val cs = MaterialTheme.colorScheme
        com.alananasss.kittytunewebsite.ui.kit.PageColumn {
            com.alananasss.kittytunewebsite.ui.kit.ViewportGate {
                Box(Modifier.fillMaxWidth()) {
                    Box(Modifier.matchParentSize()) {
                        // No alpha layer here: a graphicsLayer is sized to this (shorter) header box,
                        // so it sliced the 420 dp shape flat. Transparency goes in the colour instead,
                        // and the blob lays out at its real size.
                        Box(Modifier.align(Alignment.TopEnd).offset(x = 120.dp, y = (-40).dp).wrapContentSize(Alignment.TopEnd, unbounded = true)) {
                            com.alananasss.kittytunewebsite.ui.kit.MorphingBlob(
                                Modifier.size(420.dp),
                                color = cs.primaryContainer.copy(alpha = 0.3f), spinDegPerSecond = 7f,
                            )
                        }
                    }
                    com.alananasss.kittytunewebsite.ui.kit.ContentWidth {
                        val narrow = maxWidth < 640.dp
                        Column(Modifier.padding(top = 24.dp, bottom = 40.dp), verticalArrangement = Arrangement.spacedBy(22.dp)) {
                            com.alananasss.kittytunewebsite.ui.kit.Eyebrow("v3 · Android · Windows · macOS · Linux", Icons.Filled.Download, modifier = Modifier.reveal())
                            Text(
                                s.pageTitle,
                                style = if (narrow) MaterialTheme.typography.displayMedium else MaterialTheme.typography.displayLarge.copy(fontSize = 80.sp, lineHeight = 84.sp),
                                color = cs.onSurface,
                                modifier = Modifier.reveal(80, 70f)
                            )
                            Text(
                                s.pageSubtitle,
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.W500),
                                color = cs.onSurfaceVariant,
                                modifier = Modifier.widthIn(max = 720.dp).reveal(160)
                            )
                        }
                    }
                }
            }
            com.alananasss.kittytunewebsite.ui.kit.ContentWidth {
                val isMobile = maxWidth < 800.dp
                Column(verticalArrangement = Arrangement.spacedBy(if (isMobile) 24.dp else 32.dp), modifier = Modifier.reveal(220)) {
                    AmneziaStyleDownloadSelector(
                        selectedPlatform = selectedPlatform,
                        onPlatformSelected = { selectedPlatformName = it.name },
                        sortedPlatforms = sortedPlatforms,
                        linuxDistros = linuxDistros,
                        s = s,
                        onOpenHelp = { distroIdForHelp = it.id },
                        modifier = Modifier.fillMaxWidth()
                    )
                    if (isMobile) {
                        KofiBanner(s = s, modifier = Modifier.fillMaxWidth())
                        Changelog(s = s)
                    } else {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(24.dp), verticalAlignment = Alignment.Top) {
                            KofiBanner(s = s, modifier = Modifier.weight(1f))
                            Box(Modifier.weight(2f)) { Changelog(s = s) }
                        }
                    }
                }
            }
            Spacer(Modifier.height(120.dp))
            SiteFooter()
        }
    }

    if (distroForHelp != null) {
        CompositionLocalProvider(LocalDialogActive provides false) {
            LinuxDistroDialog(
                distro = distroForHelp,
                repo = selectedPlatform.repo,
                s = s,
                onDismiss = { distroIdForHelp = null }
            )
        }
    }
}

@Composable
private fun AmneziaStyleDownloadSelector(
    selectedPlatform: PlatformInfo,
    onPlatformSelected: (PlatformInfo) -> Unit,
    sortedPlatforms: List<PlatformInfo>,
    linuxDistros: List<LinuxDistro>,
    s: DownloadStrings,
    onOpenHelp: (LinuxDistro) -> Unit,
    modifier: Modifier = Modifier
) {
    val cs = MaterialTheme.colorScheme
    Surface(modifier = modifier.fillMaxWidth(), shape = RoundedCornerShape(40.dp), color = cs.surfaceContainerLow) {
        BoxWithConstraints(Modifier.fillMaxWidth()) {
            val narrow = maxWidth < 560.dp
            val pad = if (narrow) 16.dp else 28.dp
            Column(Modifier.padding(pad), verticalArrangement = Arrangement.spacedBy(20.dp)) {
                // platforms: one connected M3 Expressive button group (two rows on phones)
                val perRow = if (narrow) 3 else sortedPlatforms.size
                Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    val rows = sortedPlatforms.chunked(perRow)
                    rows.forEachIndexed { r, row ->
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                            row.forEachIndexed { i, platform ->
                                PlatformTile(
                                    platform, platform.name == selectedPlatform.name, narrow,
                                    position = SegmentPosition(first = i == 0, last = i == row.lastIndex, top = r == 0, bottom = r == rows.lastIndex),
                                    modifier = Modifier.weight(1f),
                                ) { onPlatformSelected(platform) }
                            }
                            repeat(perRow - row.size) { Spacer(Modifier.weight(1f)) }
                        }
                    }
                }

                if (selectedPlatform.name == "Linux") {
                    LinuxDistributionButtons(repo = selectedPlatform.repo, linuxDistros = linuxDistros, s = s, onOpenHelp = onOpenHelp)
                } else {
                    val isGithub = selectedPlatform.name == "GitHub"
                    Surface(shape = RoundedCornerShape(28.dp), color = cs.surfaceContainer, modifier = Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(if (narrow) 16.dp else 22.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                            Text(selectedPlatform.details, style = MaterialTheme.typography.bodyLarge, color = cs.onSurfaceVariant)
                            DownloadButton(selectedPlatform, isGithub, s)
                            if (selectedPlatform.name != "Android") AlternativeDownloads(selectedPlatform, s)
                        }
                    }
                }
            }
        }
    }
}

private data class SegmentPosition(val first: Boolean, val last: Boolean, val top: Boolean, val bottom: Boolean)

/**
 * One segment of the connected platform group: big outer corners, tight inner ones, and the checked
 * segment morphs to fully rounded like M3 Expressive's connected toggle buttons.
 */
@Composable
private fun PlatformTile(platform: PlatformInfo, isSelected: Boolean, narrow: Boolean, position: SegmentPosition, modifier: Modifier, onClick: () -> Unit) {
    val cs = MaterialTheme.colorScheme
    val bg by androidx.compose.animation.animateColorAsState(if (isSelected) cs.primary else cs.surfaceContainerHigh)
    val fg by androidx.compose.animation.animateColorAsState(if (isSelected) cs.onPrimary else cs.onSurface)
    val outer = if (narrow) 26.dp else 32.dp
    val inner = 8.dp
    val spec = androidx.compose.animation.core.spring<androidx.compose.ui.unit.Dp>(dampingRatio = 0.6f, stiffness = 500f)
    fun corner(isOuter: Boolean) = if (isSelected || isOuter) outer else inner
    val ts by androidx.compose.animation.core.animateDpAsState(corner(position.first && position.top), spec)
    val te by androidx.compose.animation.core.animateDpAsState(corner(position.last && position.top), spec)
    val bs by androidx.compose.animation.core.animateDpAsState(corner(position.first && position.bottom), spec)
    val be by androidx.compose.animation.core.animateDpAsState(corner(position.last && position.bottom), spec)
    val source = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(topStart = ts, topEnd = te, bottomStart = bs, bottomEnd = be),
        color = bg,
        interactionSource = source,
        modifier = modifier.height(if (narrow) 88.dp else 104.dp).springyPress(source, 0.96f, 1f),
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center, modifier = Modifier.fillMaxSize()) {
            if (platform.svgUrl != null) {
                HtmlImage(src = platform.svgUrl, description = platform.name, corner = 0.dp, fit = MediaFit.Contain, modifier = Modifier.size(if (narrow) 28.dp else 34.dp))
            } else if (platform.iconVector != null) {
                Icon(platform.iconVector, platform.name, modifier = Modifier.size(if (narrow) 28.dp else 34.dp), tint = fg)
            }
            Spacer(Modifier.height(10.dp))
            Text(platform.name, style = MaterialTheme.typography.titleSmall, color = fg, maxLines = 1)
        }
    }
}

@Composable
private fun DownloadButton(platform: PlatformInfo, isGithub: Boolean, s: DownloadStrings) {
    if (!isGithub && platform.name == "Android") {
        AndroidSplitDownloadButton(platform, s)
        return
    }
    if (!isGithub && platform.name == "macOS") {
        MacSplitDownloadButton(platform, s)
        return
    }
    Button(
        onClick = {
            if (isGithub) {
                openUrlJs("https://github.com/${platform.repo}")
            } else {
                downloadLatestStableApkByNameJs(platform.repo, platform.downloadOptions.first().second)
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
            text = if (isGithub) s.viewRepository else s.downloadPackage(platform.packageLabel),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun AndroidSplitDownloadButton(platform: PlatformInfo, s: DownloadStrings) {
    val primaryOption = platform.downloadOptions.first()
    val otherOptions = platform.downloadOptions.drop(1)
    var expanded by remember { mutableStateOf(false) }

    Box {
        SplitButtonLayout(
            leadingButton = {
                SplitButtonDefaults.LeadingButton(
                    onClick = {
                        downloadLatestStableApkByNameJs(platform.repo, primaryOption.second)
                    },
                    modifier = Modifier.height(58.dp),
                    shapes = SplitButtonDefaults.leadingButtonShapesFor(58.dp),
                    contentPadding = PaddingValues(horizontal = 24.dp, vertical = 16.dp),
                ) {
                    Icon(
                        imageVector = Icons.Default.Download,
                        contentDescription = null,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(Modifier.width(10.dp))
                    Text(
                        text = s.downloadApk,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            },
            trailingButton = {
                SplitButtonDefaults.TrailingButton(
                    checked = expanded,
                    onCheckedChange = { expanded = it },
                    modifier = Modifier.height(58.dp),
                    shapes = SplitButtonDefaults.trailingButtonShapesFor(58.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp),
                ) {
                    val rotation by animateFloatAsState(
                        targetValue = if (expanded) 180f else 0f,
                        label = "chevron rotation"
                    )
                    Icon(
                        imageVector = Icons.Default.ArrowDropDown,
                        contentDescription = s.otherApkVariants,
                        modifier = Modifier
                            .size(24.dp)
                            .graphicsLayer { rotationZ = rotation }
                    )
                }
            }
        )
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            // Header label
            Text(
                text = s.otherApkVariants,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
            )
            otherOptions.forEach { (label, nameContains) ->
                DropdownMenuItem(
                    text = { Text(label, style = MaterialTheme.typography.bodyMedium) },
                    onClick = {
                        expanded = false
                        downloadLatestStableApkByNameJs(platform.repo, nameContains)
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Download,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                )
            }
        }
    }
}

@Composable
private fun MacSplitDownloadButton(platform: PlatformInfo, s: DownloadStrings) {
    val detectedArch = remember {
        var arch = "arm"
        getDetectedArchJs { arch = it }
        arch
    }
    val isArm = detectedArch == "arm"
    val primaryOption = if (isArm) platform.downloadOptions.first() else platform.downloadOptions[1]
    val otherOptions = if (isArm) listOf(platform.downloadOptions[1]) else listOf(platform.downloadOptions.first())
    var expanded by remember { mutableStateOf(false) }

    Box {
        SplitButtonLayout(
            leadingButton = {
                SplitButtonDefaults.LeadingButton(
                    onClick = {
                        downloadLatestStableApkByNameJs(platform.repo, primaryOption.second)
                    },
                    modifier = Modifier.height(58.dp),
                    shapes = SplitButtonDefaults.leadingButtonShapesFor(58.dp),
                    contentPadding = PaddingValues(horizontal = 24.dp, vertical = 16.dp),
                ) {
                    Icon(
                        imageVector = Icons.Default.Download,
                        contentDescription = null,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(Modifier.width(10.dp))
                    Column {
                        Text(
                            text = s.downloadDmg,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = if (isArm) s.macAppleSiliconShort else s.macIntelShort,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.85f)
                        )
                    }
                }
            },
            trailingButton = {
                SplitButtonDefaults.TrailingButton(
                    checked = expanded,
                    onCheckedChange = { expanded = it },
                    modifier = Modifier.height(58.dp),
                    shapes = SplitButtonDefaults.trailingButtonShapesFor(58.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp),
                ) {
                    val rotation by animateFloatAsState(
                        targetValue = if (expanded) 180f else 0f,
                        label = "chevron rotation"
                    )
                    Icon(
                        imageVector = Icons.Default.ArrowDropDown,
                        contentDescription = s.otherMacArchitectures,
                        modifier = Modifier
                            .size(24.dp)
                            .graphicsLayer { rotationZ = rotation }
                    )
                }
            }
        )
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            Text(
                text = s.selectArchitectureDmg,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
            )
            platform.downloadOptions.forEach { (label, nameContains) ->
                DropdownMenuItem(
                    text = {
                        Column {
                            Text(label, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                            Text(
                                if (nameContains.contains("arm64")) s.macAppleSiliconDesc else s.macIntelDesc,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    },
                    onClick = {
                        expanded = false
                        downloadLatestStableApkByNameJs(platform.repo, nameContains)
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Download,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                )
            }
        }
    }
}

@Composable
private fun AlternativeDownloads(platform: PlatformInfo, s: DownloadStrings) {
    if (platform.downloadOptions.size > 1 && platform.name != "Linux") {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                s.otherVersions,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.horizontalScroll(rememberScrollState())
            ) {
                key(platform.name) {
                    platform.downloadOptions.drop(1).forEach { (label, ext) ->
                        if (platform.name == "macOS") {
                            TextButton(
                                onClick = { downloadLatestStableApkByNameJs(platform.repo, ext) },
                                shapes = ButtonDefaults.shapes()
                            ) {
                                Text(label)
                            }
                        } else {
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
    }
}

private data class LinuxDistro(
    val id: String,
    val name: String,
    val derivatives: String,
    val svgUrl: String,
    val secondarySvgUrl: String? = null,
    val packageBadge: String,
    val downloadOptions: List<DistroDownloadOption> = emptyList(),
    val command: String? = null,
    val alternativeCommand: String? = null,
    val tip: String? = null
)

private data class DistroDownloadOption(
    val label: String,
    val fileExtension: String,
    val arch: String
)

private fun buildLinuxDistros(s: DownloadStrings): List<LinuxDistro> = listOf(
    LinuxDistro(
        id = "appimage",
        name = "AppImage",
        derivatives = s.appImageDerivatives,
        svgUrl = "svg/appimage.svg",
        packageBadge = s.badgeUniversal,
        downloadOptions = listOf(
            DistroDownloadOption("x86_64", ".AppImage", "x86_64"),
            DistroDownloadOption("ARM64", ".AppImage", "arm64")
        ),
        command = "chmod +x KittyTune-*.AppImage && ./KittyTune-*.AppImage",
        tip = s.appImageTip
    ),
    LinuxDistro(
        id = "debian-ubuntu",
        name = "Debian / Ubuntu",
        derivatives = s.debianDerivatives,
        svgUrl = "svg/ubuntu.svg",
        secondarySvgUrl = "svg/debian.svg",
        packageBadge = ".deb",
        downloadOptions = listOf(
            DistroDownloadOption("amd64", ".deb", "x86_64"),
            DistroDownloadOption("arm64", ".deb", "arm64")
        ),
        command = "sudo apt install ./kitty-tune_*.deb",
        tip = s.debianTip
    ),
    LinuxDistro(
        id = "fedora-opensuse",
        name = "Fedora / openSUSE",
        derivatives = s.fedoraDerivatives,
        svgUrl = "svg/fedora.svg",
        secondarySvgUrl = "svg/opensuse.svg",
        packageBadge = ".rpm",
        downloadOptions = listOf(
            DistroDownloadOption("x86_64", ".rpm", "x86_64"),
            DistroDownloadOption("aarch64", ".rpm", "arm64")
        ),
        command = "sudo dnf install ./kitty-tune-*.rpm",
        alternativeCommand = "sudo zypper install ./kitty-tune-*.rpm",
        tip = s.fedoraTip
    ),
    LinuxDistro(
        id = "arch-linux",
        name = "Arch Linux",
        derivatives = s.archDerivatives,
        svgUrl = "svg/archlinux.svg",
        packageBadge = "AUR & Pacman",
        downloadOptions = listOf(
            DistroDownloadOption("x86_64", ".pkg.tar.zst", "x86_64"),
            DistroDownloadOption("aarch64", ".pkg.tar.zst", "arm64")
        ),
        command = "yay -S kitty-tune-bin",
        alternativeCommand = "paru -S kitty-tune-bin",
        tip = s.archTip
    ),
    LinuxDistro(
        id = "nixos",
        name = "NixOS",
        derivatives = s.nixDerivatives,
        svgUrl = "svg/nixos-snowflake.svg",
        packageBadge = "Flake",
        downloadOptions = emptyList(),
        command = "nix run github:alan7383/KittyTuneDesktop",
        alternativeCommand = "inputs.kittytune.url = \"github:alan7383/KittyTuneDesktop\";",
        tip = s.nixTip
    )
)

@Composable
private fun LinuxDistributionButtons(
    repo: String,
    linuxDistros: List<LinuxDistro>,
    s: DownloadStrings,
    onOpenHelp: (LinuxDistro) -> Unit
) {
    val detectedArch = remember {
        var arch = "x86"
        getDetectedArchJs { arch = it }
        arch
    }
    val isArm = detectedArch == "arm"
    var copiedNix by remember { mutableStateOf(false) }

    LaunchedEffect(copiedNix) {
        if (copiedNix) {
            delay(2000)
            copiedNix = false
        }
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Column(Modifier.padding(top = 4.dp, start = 4.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                text = s.supportedLinuxDistros.trimEnd(':', ' '),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = s.clickHelpForInstructions,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        BoxWithConstraints(Modifier.fillMaxWidth()) {
            val cols = when { maxWidth >= 980.dp -> 5; maxWidth >= 700.dp -> 3; maxWidth >= 420.dp -> 2; else -> 1 }
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                linuxDistros.chunked(cols).forEach { row ->
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        row.forEach { distro ->
                            DistroCard(
                                distro = distro,
                                badge = if (distro.id == "nixos" && copiedNix) s.copiedBang else distro.packageBadge,
                                helpLabel = s.instructionsFor(distro.name),
                                actionLabel = if (distro.id == "nixos") s.copy else s.downloadPackage("").trim(),
                                modifier = Modifier.weight(1f),
                                onDownload = {
                                    if (distro.id == "nixos") {
                                        copyToClipboardJs("nix run github:alan7383/KittyTuneDesktop")
                                        copiedNix = true
                                    } else {
                                        val opt = distro.downloadOptions.find {
                                            if (isArm) it.arch.contains("arm") else it.arch.contains("x86")
                                        } ?: distro.downloadOptions.firstOrNull()
                                        if (opt != null) downloadLinuxReleaseJs(repo, opt.fileExtension, opt.arch) else onOpenHelp(distro)
                                    }
                                },
                                onHelp = { onOpenHelp(distro) },
                            )
                        }
                        repeat(cols - row.size) { Spacer(Modifier.weight(1f)) }
                    }
                }
            }
        }

        Text(
            text = s.linuxFooterSummary,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

/** A distro: its logo on a tonal plate, the package format, and a connected [download | ?] pair. */
@Composable
private fun DistroCard(
    distro: LinuxDistro,
    badge: String,
    helpLabel: String,
    actionLabel: String,
    modifier: Modifier,
    onDownload: () -> Unit,
    onHelp: () -> Unit,
) {
    val cs = MaterialTheme.colorScheme
    val isNix = distro.id == "nixos"
    Surface(shape = RoundedCornerShape(28.dp), color = cs.surfaceContainer, modifier = modifier) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Box(Modifier.size(52.dp).clip(RoundedCornerShape(18.dp)).background(cs.surfaceContainerHighest), contentAlignment = Alignment.Center) {
                    if (distro.secondarySvgUrl != null) {
                        Box(Modifier.size(40.dp)) {
                            HtmlImage(src = distro.svgUrl, description = distro.name, corner = 0.dp, fit = MediaFit.Contain, modifier = Modifier.size(24.dp).align(Alignment.TopStart))
                            HtmlImage(src = distro.secondarySvgUrl, description = distro.name, corner = 0.dp, fit = MediaFit.Contain, modifier = Modifier.size(24.dp).align(Alignment.BottomEnd))
                        }
                    } else {
                        HtmlImage(src = distro.svgUrl, description = distro.name, corner = 0.dp, fit = MediaFit.Contain, modifier = Modifier.size(30.dp))
                    }
                }
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(distro.name, style = MaterialTheme.typography.titleSmall, color = cs.onSurface, maxLines = 2)
                    Surface(shape = CircleShape, color = cs.secondaryContainer) {
                        Text(badge, style = MaterialTheme.typography.labelMedium, color = cs.onSecondaryContainer, maxLines = 1, modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp))
                    }
                }
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(ButtonGroupDefaults.ConnectedSpaceBetween)) {
                Button(
                    onClick = onDownload,
                    shapes = ButtonDefaults.shapes(
                        shape = RoundedCornerShape(topStart = 20.dp, bottomStart = 20.dp, topEnd = 6.dp, bottomEnd = 6.dp),
                        pressedShape = RoundedCornerShape(12.dp),
                    ),
                    contentPadding = PaddingValues(horizontal = 12.dp),
                    modifier = Modifier.weight(1f).height(44.dp),
                ) {
                    Icon(if (isNix) Icons.Filled.ContentCopy else Icons.Filled.Download, null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(actionLabel, style = MaterialTheme.typography.labelLarge, maxLines = 1)
                }
                FilledTonalIconButton(
                    onClick = onHelp,
                    shapes = IconButtonDefaults.shapes(
                        shape = RoundedCornerShape(topStart = 6.dp, bottomStart = 6.dp, topEnd = 20.dp, bottomEnd = 20.dp),
                        pressedShape = RoundedCornerShape(12.dp),
                    ),
                    modifier = Modifier.size(width = 48.dp, height = 44.dp),
                ) {
                    Icon(Icons.Filled.HelpOutline, helpLabel, modifier = Modifier.size(20.dp))
                }
            }
        }
    }
}

/**
 * Install instructions for one distro, as an M3 Expressive sheet: logos on tonal badges, the
 * compatible distros as chips, numbered steps (download then install, or the other way round for
 * AUR and Nix), terminal-style command blocks with a morphing copy button, and the architecture this
 * computer runs highlighted.
 */
@Composable
private fun LinuxDistroDialog(
    distro: LinuxDistro,
    repo: String,
    s: DownloadStrings,
    onDismiss: () -> Unit
) {
    val cs = MaterialTheme.colorScheme
    val appear = remember { androidx.compose.animation.core.Animatable(0f) }
    LaunchedEffect(Unit) { appear.animateTo(1f, androidx.compose.animation.core.spring(dampingRatio = 0.72f, stiffness = 420f)) }
    val detectedArm = remember {
        var arch = "x86"
        getDetectedArchJs { arch = it }
        arch == "arm"
    }
    // The architecture the user picked (starts on this computer's); every command follows it.
    var selectedArm by remember { mutableStateOf(detectedArm) }
    // Real file names of the latest release, so commands are an exact copy-paste.
    var assets by remember(repo) { mutableStateOf(releaseAssetCache[repo]) }
    LaunchedEffect(repo) {
        if (assets == null) fetchReleaseAssetsJs(repo) { names ->
            val list = names.split("\n").filter { it.isNotBlank() }
            releaseAssetCache[repo] = list
            assets = list
        }
    }
    val fileFor: (String) -> String = { ext -> releaseFileName(assets, ext, selectedArm) }
    // A downloaded package is installed with the command (deb, rpm, AppImage); AUR and Nix install
    // straight from the command, the raw package being the fallback.
    val downloadFirst = distro.id in setOf("appimage", "debian-ubuntu", "fedora-opensuse")

    androidx.compose.ui.window.Dialog(
        onDismissRequest = onDismiss,
        properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Surface(
            shape = RoundedCornerShape(40.dp),
            color = cs.surfaceContainerHigh,
            modifier = Modifier
                .padding(16.dp)
                .widthIn(max = 580.dp)
                .fillMaxWidth()
                .graphicsLayer {
                    val p = appear.value
                    alpha = p.coerceIn(0f, 1f)
                    val sc = 0.9f + 0.1f * p
                    scaleX = sc; scaleY = sc
                    translationY = (1f - p) * 40f
                },
        ) {
            Column(
                Modifier.verticalScroll(rememberScrollState()).padding(28.dp),
                verticalArrangement = Arrangement.spacedBy(22.dp)
            ) {
                // ── header ──
                Row(verticalAlignment = Alignment.Top) {
                    Row(horizontalArrangement = Arrangement.spacedBy((-14).dp)) {
                        listOfNotNull(distro.svgUrl, distro.secondarySvgUrl).forEach { src ->
                            Box(
                                Modifier.size(64.dp).clip(CircleShape).background(cs.surfaceContainerHighest)
                                    .border(3.dp, cs.surfaceContainerHigh, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                HtmlImage(src = src, description = distro.name, corner = 0.dp, fit = MediaFit.Contain, modifier = Modifier.size(34.dp))
                            }
                        }
                    }
                    Spacer(Modifier.weight(1f))
                    FilledTonalIconButton(onClick = onDismiss, shapes = IconButtonDefaults.shapes(), modifier = Modifier.size(44.dp)) {
                        Icon(androidx.compose.material.icons.Icons.Rounded.Close, s.close, modifier = Modifier.size(20.dp))
                    }
                }
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(distro.name, style = MaterialTheme.typography.headlineMedium, color = cs.onSurface)
                    com.alananasss.kittytunewebsite.ui.kit.Wrap(hSpacing = 6.dp, vSpacing = 6.dp) {
                        distro.derivatives.split(",").map { it.trim().removeSuffix(".") }.filter { it.isNotEmpty() }.forEach { d ->
                            Surface(shape = CircleShape, color = cs.secondaryContainer) {
                                Text(d, style = MaterialTheme.typography.labelLarge, color = cs.onSecondaryContainer, modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp))
                            }
                        }
                    }
                }

                // ── steps ──
                val downloadStep: (@Composable () -> Unit)? = if (distro.downloadOptions.isEmpty()) null else ({
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(ButtonGroupDefaults.ConnectedSpaceBetween)) {
                        distro.downloadOptions.forEachIndexed { i, opt ->
                            val optArm = opt.arch.contains("arm") || opt.arch.contains("aarch")
                            val recommended = optArm == selectedArm
                            val shape = when {
                                distro.downloadOptions.size == 1 -> RoundedCornerShape(24.dp)
                                i == 0 -> RoundedCornerShape(topStart = 24.dp, bottomStart = 24.dp, topEnd = 8.dp, bottomEnd = 8.dp)
                                i == distro.downloadOptions.lastIndex -> RoundedCornerShape(topStart = 8.dp, bottomStart = 8.dp, topEnd = 24.dp, bottomEnd = 24.dp)
                                else -> RoundedCornerShape(8.dp)
                            }
                            val colors = if (recommended) ButtonDefaults.buttonColors() else ButtonDefaults.filledTonalButtonColors()
                            Button(
                                onClick = {
                                    selectedArm = optArm
                                    downloadLinuxReleaseJs(repo, opt.fileExtension, opt.arch)
                                },
                                shapes = ButtonDefaults.shapes(shape = shape, pressedShape = RoundedCornerShape(16.dp)),
                                colors = colors,
                                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp),
                                modifier = Modifier.weight(1f).heightIn(min = 64.dp),
                            ) {
                                Icon(Icons.Filled.Download, null, modifier = Modifier.size(20.dp))
                                Spacer(Modifier.width(10.dp))
                                Column(horizontalAlignment = Alignment.Start) {
                                    Text(opt.label, style = MaterialTheme.typography.titleSmall, maxLines = 1)
                                    Text(if (opt.label == opt.arch) opt.fileExtension.removePrefix(".") else "${opt.arch} · ${opt.fileExtension.removePrefix(".")}", style = MaterialTheme.typography.labelSmall, maxLines = 1, modifier = Modifier.graphicsLayer { alpha = 0.8f })
                                }
                                // Same layout for every segment; only the check shows on the chosen one.
                                Spacer(Modifier.weight(1f))
                                Icon(Icons.Filled.Check, null, modifier = Modifier.size(18.dp).graphicsLayer { alpha = if (recommended) 1f else 0f })
                            }
                        }
                    }
                    if (distro.id == "arch-linux") {
                        Spacer(Modifier.height(10.dp))
                        RunFromDownloadsHint(s.runFromDownloads)
                        Spacer(Modifier.height(10.dp))
                        CommandSnippetBox(title = null, command = "sudo pacman -U ./${fileFor(".pkg.tar.zst")}", s = s)
                    }
                })
                val pkgExt = distro.downloadOptions.firstOrNull()?.fileExtension
                val file = pkgExt?.let(fileFor)
                // Commands built from the exact file for the chosen architecture.
                val (mainCmd, altCmd) = when (distro.id) {
                    "appimage" -> "chmod +x $file && ./$file" to null
                    "debian-ubuntu" -> "sudo apt install ./$file" to null
                    "fedora-opensuse" -> "sudo dnf install ./$file" to "sudo zypper install ./$file"
                    else -> distro.command to distro.alternativeCommand
                }
                val commandStep: (@Composable () -> Unit)? = if (mainCmd == null) null else ({
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        if (downloadFirst) RunFromDownloadsHint(s.runFromDownloads)
                        CommandSnippetBox(
                            title = when (distro.id) {
                                "fedora-opensuse" -> s.cmdFedoraDnf
                                "arch-linux" -> s.cmdArchYay
                                "nixos" -> s.cmdNixRun
                                "appimage" -> s.cmdAppImageLaunch
                                else -> null
                            },
                            command = mainCmd,
                            s = s
                        )
                        if (altCmd != null) {
                            CommandSnippetBox(
                                title = when (distro.id) {
                                    "fedora-opensuse" -> s.cmdOpenSuseZypper
                                    "arch-linux" -> s.cmdArchParu
                                    "nixos" -> s.cmdNixFlakeInput
                                    else -> s.cmdAlternative
                                },
                                command = altCmd,
                                s = s,
                                prompt = if (distro.id == "nixos") null else "$"
                            )
                        }
                    }
                })
                val steps = buildList {
                    val dl = downloadStep?.let { s.directPackageDownload.trimEnd(':', ' ', '\u00A0') to it }
                    val cmd = commandStep?.let { s.cmdTerminal to it }
                    if (downloadFirst) { dl?.let(::add); cmd?.let(::add) } else { cmd?.let(::add); dl?.let(::add) }
                }
                steps.forEachIndexed { i, (title, body) ->
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            Box(
                                Modifier.size(32.dp).clip(com.alananasss.kittytunewebsite.ui.kit.PolygonShape(androidx.compose.material3.MaterialShapes.Cookie9Sided)).background(cs.primary),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("${i + 1}", style = MaterialTheme.typography.titleSmall, color = cs.onPrimary)
                            }
                            Text(title, style = MaterialTheme.typography.titleMedium, color = cs.onSurface)
                        }
                        body()
                    }
                }

                // ── tip ──
                if (distro.tip != null) {
                    Row(
                        Modifier.fillMaxWidth().clip(RoundedCornerShape(24.dp)).background(cs.tertiaryContainer).padding(16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Icon(androidx.compose.material.icons.Icons.Rounded.Lightbulb, null, tint = cs.onTertiaryContainer, modifier = Modifier.size(20.dp))
                        Text(distro.tip, style = MaterialTheme.typography.bodyMedium, color = cs.onTertiaryContainer)
                    }
                }

                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    Button(onClick = onDismiss, shapes = ButtonDefaults.shapes(), modifier = Modifier.height(48.dp), contentPadding = PaddingValues(horizontal = 28.dp)) {
                        Text(s.close, style = MaterialTheme.typography.titleSmall)
                    }
                }
            }
        }
    }
}

@Composable
private fun RunFromDownloadsHint(text: String) {
    val cs = MaterialTheme.colorScheme
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(start = 4.dp)) {
        Icon(androidx.compose.material.icons.Icons.Rounded.FolderOpen, null, tint = cs.primary, modifier = Modifier.size(18.dp))
        Text(text, style = MaterialTheme.typography.bodyMedium, color = cs.onSurfaceVariant)
    }
}

private val releaseAssetCache = HashMap<String, List<String>>()

/**
 * The release file for [ext] and the chosen architecture: the real name from the latest GitHub
 * release when it has loaded, otherwise the release naming pattern with a wildcard version.
 */
private fun releaseFileName(assets: List<String>?, ext: String, arm: Boolean): String {
    val e = ext.lowercase()
    val keys = if (arm) listOf("aarch64", "arm64") else listOf("x86_64", "amd64", "x64")
    assets?.firstOrNull { n -> n.lowercase().endsWith(e) && keys.any { n.lowercase().contains(it) } }?.let { return it }
    return when (e) {
        ".appimage" -> "KittyTune-*-${if (arm) "aarch64" else "x86_64"}.AppImage"
        ".deb" -> "kitty-tune_*_${if (arm) "arm64" else "amd64"}.deb"
        ".rpm" -> "kitty-tune-*.${if (arm) "aarch64" else "x86_64"}.rpm"
        else -> "kitty-tune-*-${if (arm) "aarch64" else "x86_64"}$ext"
    }
}

private fun fetchReleaseAssetsJs(repo: String, onResult: (String) -> Unit) {
    js("""
        fetch('https://api.github.com/repos/' + repo + '/releases/latest')
            .then(function(r) { if (!r.ok) throw new Error('HTTP ' + r.status); return r.json(); })
            .then(function(d) { onResult((d.assets || []).map(function(a) { return a.name; }).join('\n')); })
            .catch(function() {});
    """)
}

/** A terminal-style command line with a copy button that morphs into a check once copied. */
@Composable
private fun CommandSnippetBox(
    title: String?,
    command: String,
    s: DownloadStrings,
    prompt: String? = "$"
) {
    val cs = MaterialTheme.colorScheme
    var copied by remember { mutableStateOf(false) }
    LaunchedEffect(copied) {
        if (copied) {
            delay(1800)
            copied = false
        }
    }
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        if (title != null) {
            Text(title, style = MaterialTheme.typography.labelLarge, color = cs.onSurfaceVariant, modifier = Modifier.padding(start = 4.dp))
        }
        Row(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(24.dp)).background(cs.surfaceContainerLowest).padding(start = 18.dp, end = 8.dp, top = 8.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            if (prompt != null) {
                Text(prompt, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, color = cs.primary, style = MaterialTheme.typography.bodyLarge)
            }
            Box(Modifier.weight(1f).horizontalScroll(rememberScrollState())) {
                Text(command, fontFamily = FontFamily.Monospace, color = cs.onSurface, style = MaterialTheme.typography.bodyLarge, softWrap = false)
            }
            val bg by androidx.compose.animation.animateColorAsState(if (copied) cs.primary else cs.secondaryContainer)
            val fg by androidx.compose.animation.animateColorAsState(if (copied) cs.onPrimary else cs.onSecondaryContainer)
            val corner by androidx.compose.animation.core.animateDpAsState(
                if (copied) 14.dp else 22.dp, androidx.compose.animation.core.spring(dampingRatio = 0.5f, stiffness = 500f)
            )
            Box(
                Modifier.size(44.dp).clip(RoundedCornerShape(corner)).background(bg).clickable {
                    copyToClipboardJs(command)
                    copied = true
                },
                contentAlignment = Alignment.Center
            ) {
                androidx.compose.animation.AnimatedContent(copied, label = "copy") { c ->
                    Icon(
                        if (c) Icons.Filled.Check else Icons.Filled.ContentCopy,
                        if (c) s.copied else s.copy,
                        tint = fg, modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun KofiBanner(s: DownloadStrings, modifier: Modifier = Modifier) {
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
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = s.kofiTitle,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Black,
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center
                )
                Box(modifier = Modifier.size(28.dp)) {
                    HtmlImage(
                        src = "svg/kofi_symbol.svg",
                        description = s.kofiIconAlt,
                        corner = 0.dp,
                        fit = MediaFit.Contain,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
            Spacer(Modifier.height(16.dp))
            Text(
                text = s.kofiBody,
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
                Box(modifier = Modifier.height(20.dp).width(74.dp)) {
                    HtmlImage(
                        src = "svg/kofi_logo.svg",
                        description = "Ko-fi",
                        corner = 0.dp,
                        fit = MediaFit.Contain,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
        }
    }
}

@Composable
private fun Changelog(s: DownloadStrings) {
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
                s.latestReleases,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Black,
                color = MaterialTheme.colorScheme.onSurface
            )

            if (androidRelease == null && desktopRelease == null) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp))
                    Text(s.loadingReleaseNotes, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            } else {
                androidRelease?.let { ReleaseItem(it.copy(channel = s.channelAndroidApk)) }
                if (androidRelease != null && desktopRelease != null) {
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                }
                desktopRelease?.let { ReleaseItem(it.copy(channel = s.channelDesktop)) }
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
            ReleaseNotesBody(release.body, modifier)
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

private val commitLinkRegex = Regex("""\s*\(\[[0-9a-f]{6,40}]\([^)]*\)\)""")
private val mdLinkRegex = Regex("""\[([^\]]+)]\([^)]*\)""")

/** Renders GitHub release markdown as headed sections with bullet rows (commit links stripped). */
@Composable
private fun ReleaseNotesBody(body: String, modifier: Modifier = Modifier) {
    val cs = MaterialTheme.colorScheme
    val lines = remember(body) {
        body.lines().map { it.trimEnd() }.filter { it.isNotBlank() }.map { line ->
            line.replace(commitLinkRegex, "").replace(mdLinkRegex, "$1").replace("**", "").replace("`", "")
        }
    }
    Column(modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        lines.forEach { line ->
            val t = line.trimStart()
            when {
                t.startsWith("#") -> Text(
                    t.trimStart('#').trim(),
                    style = MaterialTheme.typography.titleSmall,
                    color = cs.primary,
                    modifier = Modifier.padding(top = 6.dp)
                )
                t.startsWith("* ") || t.startsWith("- ") -> Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Box(Modifier.padding(top = 8.dp).size(6.dp).clip(androidx.compose.foundation.shape.CircleShape).background(cs.tertiary))
                    Text(t.drop(2), style = MaterialTheme.typography.bodyMedium, color = cs.onSurface)
                }
                else -> Text(t, style = MaterialTheme.typography.bodyMedium, color = cs.onSurfaceVariant)
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

private fun getOsJs(onResult: (String) -> Unit) {
    js("""
        let ua = window.navigator.userAgent;
        if (/android/i.test(ua)) { onResult("Android"); }
        else if (/Win/i.test(ua)) { onResult("Windows"); }
        else if (/Mac/i.test(ua)) { onResult("macOS"); }
        else if (/Linux/i.test(ua)) { onResult("Linux"); }
        else { onResult("Windows"); }
    """)
}

private fun getDetectedArchJs(onResult: (String) -> Unit) {
    js("""
        let ua = (window.navigator.userAgent || '').toLowerCase();
        let isArm = ua.includes('arm') || ua.includes('aarch64');
        try {
            let canvas = document.createElement('canvas');
            let gl = canvas.getContext('webgl') || canvas.getContext('experimental-webgl');
            if (gl) {
                let debugInfo = gl.getExtension('WEBGL_debug_renderer_info');
                if (debugInfo) {
                    let renderer = (gl.getParameter(debugInfo.UNMASKED_RENDERER_WEBGL) || '').toLowerCase();
                    if (renderer.includes('apple')) isArm = true;
                }
            }
        } catch (e) {}
        onResult(isArm ? "arm" : "x86");
    """)
}

private fun copyToClipboardJs(text: String) {
    js("""
        function execCopy(val) {
            try {
                const ta = document.createElement('textarea');
                ta.value = val;
                ta.setAttribute('readonly', '');
                ta.style.position = 'fixed';
                ta.style.left = '0';
                ta.style.top = '0';
                ta.style.opacity = '0';
                ta.style.pointerEvents = 'none';
                ta.style.fontSize = '16px';
                document.body.appendChild(ta);
                ta.select();
                ta.setSelectionRange(0, 99999);
                const successful = document.execCommand('copy');
                document.body.removeChild(ta);
                return successful;
            } catch (err) {
                return false;
            }
        }

        const syncd = execCopy(text);

        if (navigator.clipboard && navigator.clipboard.writeText) {
            navigator.clipboard.writeText(text).catch(function(err) {
                if (!syncd) {
                    console.warn('Clipboard copy failed:', err);
                }
            });
        }
    """)
}

private fun downloadLinuxReleaseJs(repo: String, ext: String, arch: String) {
    js("""
        fetch('https://api.github.com/repos/' + repo + '/releases/latest')
            .then(res => {
                if (!res.ok) throw new Error('HTTP ' + res.status);
                return res.json();
            })
            .then(data => {
                let url = null;
                if (data.assets && data.assets.length > 0) {
                    let targetArch = '';
                    if (arch === 'arm64' || arch === 'aarch64') {
                        targetArch = (ext.toLowerCase() === '.deb') ? 'arm64' : 'aarch64';
                    } else {
                        targetArch = (ext.toLowerCase() === '.deb') ? 'amd64' : 'x86_64';
                    }
                    const extLower = ext.toLowerCase();
                    const isArchMatch = function(name) {
                        const n = name.toLowerCase();
                        if (targetArch === 'x86_64') {
                            return n.includes('x86_64') || n.includes('x64') || n.includes('amd64');
                        }
                        if (targetArch === 'amd64') {
                            return n.includes('amd64') || n.includes('x86_64') || n.includes('x64');
                        }
                        if (targetArch === 'arm64' || targetArch === 'aarch64') {
                            return n.includes('arm64') || n.includes('aarch64');
                        }
                        return n.includes(targetArch);
                    };
                    const asset = data.assets.find(a => a.name.toLowerCase().endsWith(extLower) && isArchMatch(a.name))
                               || data.assets.find(a => a.name.toLowerCase().endsWith(extLower));
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

private fun downloadLatestReleaseJs(repo: String, ext: String) {
    js("""
        fetch('https://api.github.com/repos/' + repo + '/releases/latest')
            .then(res => {
                if (!res.ok) throw new Error('HTTP ' + res.status);
                return res.json();
            })
            .then(data => {
                let url = null;
                if (data.assets && data.assets.length > 0) {
                    let ua = (window.navigator.userAgent || '').toLowerCase();
                    let isArm = ua.includes('arm') || ua.includes('aarch64');
                    let extLower = ext.toLowerCase();
                    let targetArch = isArm ? (extLower === '.deb' ? 'arm64' : 'aarch64') : (extLower === '.deb' ? 'amd64' : 'x86_64');

                    const asset = data.assets.find(a => a.name.toLowerCase().endsWith(extLower) && (a.name.toLowerCase().includes(targetArch) || (targetArch === 'x86_64' && a.name.toLowerCase().includes('x64'))))
                               || data.assets.find(a => a.name.toLowerCase().endsWith(extLower));
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

// Fetches the first non-prerelease, non-draft release and downloads the asset
// whose filename contains `nameContains`. Used for Android APKs so we can
// distinguish arm64-v8a / armeabi-v7a / universal / x86_64 by name substring.
private fun downloadLatestStableApkByNameJs(repo: String, nameContains: String) {
    js("""
        fetch('https://api.github.com/repos/' + repo + '/releases?per_page=10')
            .then(res => {
                if (!res.ok) throw new Error('HTTP ' + res.status);
                return res.json();
            })
            .then(releases => {
                const stable = releases.find(r => !r.prerelease && !r.draft);
                if (!stable) {
                    window.open('https://github.com/' + repo + '/releases', '_blank');
                    return;
                }
                const asset = (stable.assets || []).find(a => a.name.includes(nameContains));
                if (asset) {
                    const a = document.createElement('a');
                    a.href = asset.browser_download_url;
                    a.download = '';
                    document.body.appendChild(a);
                    a.click();
                    document.body.removeChild(a);
                } else {
                    window.open('https://github.com/' + repo + '/releases/tag/' + stable.tag_name, '_blank');
                }
            })
            .catch(e => {
                window.open('https://github.com/' + repo + '/releases', '_blank');
            });
    """)
}
