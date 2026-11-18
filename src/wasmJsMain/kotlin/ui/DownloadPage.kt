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
import androidx.compose.material3.OutlinedButton
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
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
                    title = s.pageTitle,
                    description = s.pageSubtitle
                )

                if (isMobile) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(24.dp)
                    ) {
                        AmneziaStyleDownloadSelector(
                            selectedPlatform = selectedPlatform,
                            onPlatformSelected = { selectedPlatformName = it.name },
                            sortedPlatforms = sortedPlatforms,
                            linuxDistros = linuxDistros,
                            s = s,
                            onOpenHelp = { distroIdForHelp = it.id },
                            modifier = Modifier.fillMaxWidth()
                        )
                        KofiBanner(s = s, modifier = Modifier.fillMaxWidth())
                    }
                } else {
                    Row(
                        modifier = Modifier.fillMaxWidth().height(295.dp),
                        horizontalArrangement = Arrangement.spacedBy(32.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(modifier = Modifier.weight(2f).fillMaxHeight()) {
                            AmneziaStyleDownloadSelector(
                                selectedPlatform = selectedPlatform,
                                onPlatformSelected = { selectedPlatformName = it.name },
                                sortedPlatforms = sortedPlatforms,
                                linuxDistros = linuxDistros,
                                s = s,
                                onOpenHelp = { distroIdForHelp = it.id },
                                modifier = Modifier.fillMaxHeight()
                            )
                        }
                        Box(modifier = Modifier.weight(1f).fillMaxHeight()) {
                            KofiBanner(s = s, modifier = Modifier.fillMaxHeight())
                        }
                    }
                }
                Changelog(s = s)
            }
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

    BoxWithConstraints(modifier = modifier.fillMaxWidth()) {
        val isMobile = maxWidth < 600.dp
        val horizontalPadding = if (isMobile) 16.dp else 28.dp
        val verticalPadding = if (isMobile) 14.dp else 18.dp

        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(32.dp),
            color = MaterialTheme.colorScheme.surfaceContainerLow
        ) {
            Column(
                modifier = Modifier.padding(vertical = verticalPadding, horizontal = horizontalPadding),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
            // OS selection row
            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                sortedPlatforms.forEach { platform ->
                    val isSelected = platform.name == selectedPlatform.name
                    val borderColor = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
                    val bgColor = if (isSelected) MaterialTheme.colorScheme.surfaceContainerHighest else Color.Transparent

                    Surface(
                        onClick = { onPlatformSelected(platform) },
                        shape = RoundedCornerShape(24.dp),
                        color = bgColor,
                        border = BorderStroke(if (isSelected) 2.dp else 1.dp, borderColor),
                        modifier = Modifier.size(width = 110.dp, height = 106.dp)
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
            if (selectedPlatform.name == "Linux") {
                LinuxDistributionButtons(
                    repo = selectedPlatform.repo,
                    linuxDistros = linuxDistros,
                    s = s,
                    onOpenHelp = onOpenHelp
                )
            } else {
                BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
                    val isMobile = maxWidth < 600.dp
                    val isGithub = selectedPlatform.name == "GitHub"

                    if (isMobile) {
                        Column(
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            DownloadButton(selectedPlatform, isGithub, s)
                            if (selectedPlatform.name != "Android") {
                                AlternativeDownloads(selectedPlatform, s)
                            }
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
                                DownloadButton(selectedPlatform, isGithub, s)
                                Text(
                                    text = selectedPlatform.details,
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            if (selectedPlatform.name != "Android") {
                                AlternativeDownloads(selectedPlatform, s)
                            }
                        }
                    }
                }
            }
        }
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
        svgUrl = "svg/nixos.svg",
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
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = s.supportedLinuxDistros,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = s.clickHelpForInstructions,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            linuxDistros.forEach { distro ->
                SplitButtonLayout(
                    leadingButton = {
                        SplitButtonDefaults.LeadingButton(
                            onClick = {
                                if (distro.id == "nixos") {
                                    copyToClipboardJs("nix run github:alan7383/KittyTuneDesktop")
                                    copiedNix = true
                                } else {
                                    val opt = distro.downloadOptions.find {
                                        if (isArm) it.arch.contains("arm") else it.arch.contains("x86")
                                    } ?: distro.downloadOptions.firstOrNull()
                                    if (opt != null) {
                                        downloadLinuxReleaseJs(repo, opt.fileExtension, opt.arch)
                                    } else {
                                        onOpenHelp(distro)
                                    }
                                }
                            },
                            modifier = Modifier.height(52.dp),
                            shapes = SplitButtonDefaults.leadingButtonShapesFor(52.dp),
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                if (distro.secondarySvgUrl != null) {
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(2.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Box(modifier = Modifier.size(20.dp)) {
                                            HtmlImage(src = distro.svgUrl, description = distro.name, corner = 0.dp, fit = MediaFit.Contain, modifier = Modifier.fillMaxSize())
                                        }
                                        Box(modifier = Modifier.size(20.dp)) {
                                            HtmlImage(src = distro.secondarySvgUrl, description = distro.name, corner = 0.dp, fit = MediaFit.Contain, modifier = Modifier.fillMaxSize())
                                        }
                                    }
                                } else {
                                    Box(modifier = Modifier.size(22.dp)) {
                                        HtmlImage(src = distro.svgUrl, description = distro.name, corner = 0.dp, fit = MediaFit.Contain, modifier = Modifier.fillMaxSize())
                                    }
                                }

                                Column {
                                    Text(
                                        text = distro.name,
                                        style = MaterialTheme.typography.labelLarge,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = if (distro.id == "nixos" && copiedNix) s.copiedBang else distro.packageBadge,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.85f)
                                    )
                                }
                            }
                        }
                    },
                    trailingButton = {
                        SplitButtonDefaults.TrailingButton(
                            checked = false,
                            onCheckedChange = { onOpenHelp(distro) },
                            modifier = Modifier.height(52.dp),
                            shapes = SplitButtonDefaults.trailingButtonShapesFor(52.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.HelpOutline,
                                contentDescription = s.instructionsFor(distro.name),
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                )
            }
        }

        Text(
            text = s.linuxFooterSummary,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun LinuxDistroDialog(
    distro: LinuxDistro,
    repo: String,
    s: DownloadStrings,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier.widthIn(max = 540.dp).fillMaxWidth(),
        title = {
            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(modifier = Modifier.size(28.dp)) {
                    HtmlImage(src = distro.svgUrl, description = distro.name, corner = 0.dp, fit = MediaFit.Contain, modifier = Modifier.fillMaxSize())
                }
                if (distro.secondarySvgUrl != null) {
                    Box(modifier = Modifier.size(28.dp)) {
                        HtmlImage(src = distro.secondarySvgUrl, description = distro.name, corner = 0.dp, fit = MediaFit.Contain, modifier = Modifier.fillMaxSize())
                    }
                }
                Text(
                    text = s.installationTitle(distro.name),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text(
                    text = distro.derivatives,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                // Main Command
                if (distro.command != null) {
                    CommandSnippetBox(
                        title = when (distro.id) {
                            "fedora-opensuse" -> s.cmdFedoraDnf
                            "arch-linux" -> s.cmdArchYay
                            "nixos" -> s.cmdNixRun
                            "appimage" -> s.cmdAppImageLaunch
                            else -> s.cmdTerminal
                        },
                        command = distro.command,
                        s = s
                    )
                }

                // Alternative Command
                if (distro.alternativeCommand != null) {
                    CommandSnippetBox(
                        title = when (distro.id) {
                            "fedora-opensuse" -> s.cmdOpenSuseZypper
                            "arch-linux" -> s.cmdArchParu
                            "nixos" -> s.cmdNixFlakeInput
                            else -> s.cmdAlternative
                        },
                        command = distro.alternativeCommand,
                        s = s,
                        prompt = if (distro.id == "nixos") null else "$"
                    )
                }

                // Tip note
                if (distro.tip != null) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.4f),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.secondary.copy(alpha = 0.25f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Info,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                                tint = MaterialTheme.colorScheme.secondary
                            )
                            Text(
                                text = distro.tip,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }

                // Direct download buttons
                if (distro.downloadOptions.isNotEmpty()) {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = s.directPackageDownload,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            distro.downloadOptions.forEach { opt ->
                                OutlinedButton(
                                    onClick = {
                                        downloadLinuxReleaseJs(repo, opt.fileExtension, opt.arch)
                                    },
                                    shapes = ButtonDefaults.shapes(),
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.width(6.dp))
                                    Text(
                                        text = "${opt.label} (${opt.arch})",
                                        style = MaterialTheme.typography.labelSmall
                                    )
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                shapes = ButtonDefaults.shapes()
            ) {
                Text(s.close)
            }
        }
    )
}

@Composable
private fun CommandSnippetBox(
    title: String,
    command: String,
    s: DownloadStrings,
    prompt: String? = "$"
) {
    var copied by remember { mutableStateOf(false) }

    LaunchedEffect(copied) {
        if (copied) {
            delay(2000)
            copied = false
        }
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Surface(
            shape = RoundedCornerShape(12.dp),
            color = MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.9f),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (prompt != null) {
                        Text(
                            text = prompt,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                    Box(
                        modifier = Modifier
                            .weight(1f, fill = false)
                            .horizontalScroll(rememberScrollState())
                    ) {
                        Text(
                            text = command,
                            fontFamily = FontFamily.Monospace,
                            color = MaterialTheme.colorScheme.onSurface,
                            style = MaterialTheme.typography.bodyMedium,
                            softWrap = false
                        )
                    }
                }

                Spacer(Modifier.width(8.dp))

                Button(
                    onClick = {
                        copyToClipboardJs(command)
                        copied = true
                    },
                    shapes = ButtonDefaults.shapes(),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                    colors = if (copied) {
                        ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer,
                            contentColor = MaterialTheme.colorScheme.primary
                        )
                    } else {
                        ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                            contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    },
                    modifier = Modifier.height(34.dp)
                ) {
                    Icon(
                        imageVector = if (copied) Icons.Default.Check else Icons.Default.ContentCopy,
                        contentDescription = if (copied) s.copied else s.copy,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(
                        text = if (copied) s.copiedBang else s.copy,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold
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
