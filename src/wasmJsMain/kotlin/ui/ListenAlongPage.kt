@file:OptIn(
    androidx.compose.material3.ExperimentalMaterial3Api::class,
    androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class,
    kotlin.js.ExperimentalWasmJsInterop::class,
)

package com.alananasss.kittytunewebsite.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DesktopWindows
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Headphones
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Podcasts
import androidx.compose.material.icons.filled.QueueMusic
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ButtonGroupDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.ToggleButton
import androidx.compose.material3.ToggleButtonDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.browser.window
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

private enum class QueueViewTab { UPCOMING, PLAYLIST }

@Composable
fun ListenAlongPage(
    activeCode: String?,
    onCodeSelected: (String) -> Unit,
    onDownloadClick: () -> Unit,
) {
    var isFr by remember { mutableStateOf(runCatching { isFrenchBrowserJs() }.getOrDefault(false)) }
    val normalizedCode = remember(activeCode) {
        activeCode?.let(TogetherWebLinks::normalizeCode)?.takeIf { it.length == TogetherWebLinks.CODE_LENGTH }
    }

    var session by remember(normalizedCode) {
        mutableStateOf(TogetherWebSessionState(code = normalizedCode.orEmpty()))
    }
    var nowMs by remember { mutableLongStateOf(nowEpochMsJs()) }
    var codeInput by remember(normalizedCode) {
        mutableStateOf(normalizedCode?.let(TogetherWebLinks::displayCode).orEmpty())
    }
    var copiedCode by remember { mutableStateOf(false) }
    var copiedLink by remember { mutableStateOf(false) }
    var joinAttempted by remember(normalizedCode) { mutableStateOf(false) }
    var queueTab by remember { mutableStateOf(QueueViewTab.UPCOMING) }
    val scope = rememberCoroutineScope()

    // Connect live to the encrypted MQTT-over-WSS relay whenever a valid 8-char room code is active
    DisposableEffect(normalizedCode) {
        if (normalizedCode != null) {
            session = TogetherWebSessionState(
                code = normalizedCode,
                connection = WebRelayConnection.CONNECTING,
            )
            startTogetherWebRelayJs(normalizedCode) { rawEvent ->
                val ts = nowEpochMsJs()
                nowMs = ts
                session = reduceTogetherWebEvent(session, rawEvent, ts)
            }
        } else {
            stopTogetherWebRelayJs()
        }
        onDispose {
            stopTogetherWebRelayJs()
        }
    }

    // Real-time clock tick for live playback progress & presence freshness
    LaunchedEffect(normalizedCode, session.isPlaying, session.connection) {
        while (normalizedCode != null) {
            delay(if (session.isPlaying) 250L else 2000L)
            nowMs = nowEpochMsJs()
        }
    }

    val cs = MaterialTheme.colorScheme

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val isMobile = maxWidth < 680.dp
        val isCompact = maxWidth < 980.dp
        val pageHorizontalPad = if (isMobile) 16.dp else 48.dp

        DesktopScrollableColumn(
            modifier = Modifier.fillMaxSize(),
            columnModifier = Modifier
                .padding(horizontal = pageHorizontalPad)
                .padding(top = if (isMobile) 20.dp else 44.dp, bottom = 96.dp),
            verticalArrangement = Arrangement.spacedBy(if (isMobile) 24.dp else 40.dp),
        ) {
            // Top Utility Row: Language Toggle + End-to-End Encryption Pill
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Surface(
                    shape = CircleShape,
                    color = cs.surfaceContainerHigh,
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Security,
                            contentDescription = null,
                            tint = cs.primary,
                            modifier = Modifier.size(16.dp),
                        )
                        Text(
                            text = if (isFr) {
                                "Session chiffrée de bout en bout · AES-256-GCM"
                            } else {
                                "End-to-end encrypted session · AES-256-GCM"
                            },
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = cs.onSurfaceVariant,
                        )
                    }
                }

                Surface(
                    shape = CircleShape,
                    color = cs.secondaryContainer,
                    modifier = Modifier
                        .clip(CircleShape)
                        .clickable { isFr = !isFr },
                ) {
                    Text(
                        text = if (isFr) "FR · EN" else "EN · FR",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.ExtraBold,
                        color = cs.onSecondaryContainer,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                    )
                }
            }

            if (normalizedCode != null) {
                // ─── 1. LIVE INVITE HERO ISLAND (Material 3 Expressive, Zero Gradients) ───
                val isLive = session.hasLiveHost(nowMs) || session.nowPlaying != null
                val hostName = session.resolvedHostName()
                val formattedCode = TogetherWebLinks.displayCode(normalizedCode)
                val listeners = session.activeListeners(nowMs)

                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(if (isMobile) 32.dp else 44.dp),
                    color = cs.surfaceContainerLow,
                ) {
                    val innerPad = if (isMobile) 20.dp else 40.dp
                    if (isCompact) {
                        Column(
                            modifier = Modifier.padding(innerPad),
                            verticalArrangement = Arrangement.spacedBy(24.dp),
                        ) {
                            InvitePromptColumn(
                                isFr = isFr,
                                isMobile = isMobile,
                                isLive = isLive,
                                hostName = hostName,
                                formattedCode = formattedCode,
                                session = session,
                                copiedCode = copiedCode,
                                copiedLink = copiedLink,
                                joinAttempted = joinAttempted,
                                onJoinInApp = {
                                    joinAttempted = true
                                    triggerAppDeepLinkJs(TogetherWebLinks.appDeepLink(normalizedCode, autoJoin = true))
                                },
                                onCopyCode = {
                                    copyTextWebJs(formattedCode)
                                    copiedCode = true
                                    scope.launch { delay(2200); copiedCode = false }
                                },
                                onCopyLink = {
                                    val base = window.location.origin + window.location.pathname.substringBeforeLast("/", "")
                                    copyTextWebJs(TogetherWebLinks.webShareUrl(base, normalizedCode))
                                    copiedLink = true
                                    scope.launch { delay(2200); copiedLink = false }
                                },
                                onDownloadClick = onDownloadClick,
                            )

                            LivePlayerShowcaseCard(
                                isFr = isFr,
                                isMobile = isMobile,
                                isLive = isLive,
                                session = session,
                                listeners = listeners,
                                nowMs = nowMs,
                            )
                        }
                    } else {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(innerPad),
                            horizontalArrangement = Arrangement.spacedBy(40.dp),
                            verticalAlignment = Alignment.Top,
                        ) {
                            Box(modifier = Modifier.weight(1.08f)) {
                                InvitePromptColumn(
                                    isFr = isFr,
                                    isMobile = false,
                                    isLive = isLive,
                                    hostName = hostName,
                                    formattedCode = formattedCode,
                                    session = session,
                                    copiedCode = copiedCode,
                                    copiedLink = copiedLink,
                                    joinAttempted = joinAttempted,
                                    onJoinInApp = {
                                        joinAttempted = true
                                        triggerAppDeepLinkJs(TogetherWebLinks.appDeepLink(normalizedCode, autoJoin = true))
                                    },
                                    onCopyCode = {
                                        copyTextWebJs(formattedCode)
                                        copiedCode = true
                                        scope.launch { delay(2200); copiedCode = false }
                                    },
                                    onCopyLink = {
                                        val base = window.location.origin + window.location.pathname.substringBeforeLast("/", "")
                                        copyTextWebJs(TogetherWebLinks.webShareUrl(base, normalizedCode))
                                        copiedLink = true
                                        scope.launch { delay(2200); copiedLink = false }
                                    },
                                    onDownloadClick = onDownloadClick,
                                )
                            }

                            Box(modifier = Modifier.weight(0.92f)) {
                                LivePlayerShowcaseCard(
                                    isFr = isFr,
                                    isMobile = false,
                                    isLive = isLive,
                                    session = session,
                                    listeners = listeners,
                                    nowMs = nowMs,
                                )
                            }
                        }
                    }
                }

                // ─── 2. LIVE QUEUE & SHARED PLAYLIST SECTION ──────────────────────────────
                if (session.upcoming.isNotEmpty() || session.playlist.isNotEmpty()) {
                    val effectiveTab = when {
                        queueTab == QueueViewTab.UPCOMING && session.upcoming.isEmpty() && session.playlist.isNotEmpty() ->
                            QueueViewTab.PLAYLIST
                        queueTab == QueueViewTab.PLAYLIST && session.playlist.isEmpty() && session.upcoming.isNotEmpty() ->
                            QueueViewTab.UPCOMING
                        else -> queueTab
                    }

                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(36.dp),
                        color = cs.surfaceContainerLow,
                    ) {
                        Column(
                            modifier = Modifier.padding(if (isMobile) 20.dp else 32.dp),
                            verticalArrangement = Arrangement.spacedBy(20.dp),
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text(
                                    text = if (isFr) "File d'attente en direct" else "Live Session Queue",
                                    style = if (isMobile) MaterialTheme.typography.titleLarge else MaterialTheme.typography.headlineSmall,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = cs.onSurface,
                                )

                                if (session.upcoming.isNotEmpty() && session.playlist.isNotEmpty()) {
                                    Row(
                                        modifier = Modifier
                                            .widthIn(max = 340.dp)
                                            .clip(RoundedCornerShape(20.dp))
                                            .background(cs.surfaceContainer)
                                            .padding(4.dp),
                                        horizontalArrangement = Arrangement.spacedBy(ButtonGroupDefaults.ConnectedSpaceBetween),
                                    ) {
                                        val opts = listOf(
                                            QueueViewTab.UPCOMING to (if (isFr) "À suivre (${session.upcoming.size})" else "Up Next (${session.upcoming.size})"),
                                            QueueViewTab.PLAYLIST to "Playlist (${session.playlist.size})",
                                        )
                                        opts.forEachIndexed { idx, (tab, label) ->
                                            ToggleButton(
                                                checked = effectiveTab == tab,
                                                onCheckedChange = { queueTab = tab },
                                                shapes = if (idx == 0) {
                                                    ButtonGroupDefaults.connectedLeadingButtonShapes()
                                                } else {
                                                    ButtonGroupDefaults.connectedTrailingButtonShapes()
                                                },
                                                colors = ToggleButtonDefaults.toggleButtonColors(
                                                    containerColor = cs.surfaceContainerHigh,
                                                    contentColor = cs.onSurfaceVariant,
                                                    checkedContainerColor = cs.primary,
                                                    checkedContentColor = cs.onPrimary,
                                                ),
                                            ) {
                                                Text(label, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                                            }
                                        }
                                    }
                                }
                            }

                            val tracksToShow = if (effectiveTab == QueueViewTab.UPCOMING) {
                                session.upcoming
                            } else {
                                session.playlist
                            }

                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(28.dp)),
                                verticalArrangement = Arrangement.spacedBy(2.dp),
                            ) {
                                tracksToShow.take(16).forEachIndexed { index, track ->
                                    WebExpressiveGroupRow {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(horizontal = 16.dp, vertical = 12.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(14.dp),
                                        ) {
                                            Text(
                                                text = (index + 1).toString(),
                                                style = MaterialTheme.typography.labelLarge,
                                                fontWeight = FontWeight.ExtraBold,
                                                color = cs.onSurfaceVariant,
                                                modifier = Modifier.width(28.dp),
                                            )
                                            WebTrackArtwork(
                                                url = track.artworkUrl,
                                                title = track.title,
                                                size = 48.dp,
                                                corner = 12.dp,
                                            )
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    text = track.title.ifBlank { if (isFr) "Titre sans nom" else "Untitled track" },
                                                    style = MaterialTheme.typography.bodyLarge,
                                                    fontWeight = FontWeight.Bold,
                                                    color = cs.onSurface,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis,
                                                )
                                                Text(
                                                    text = track.artist.ifBlank { "KittyTune" },
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = cs.onSurfaceVariant,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis,
                                                )
                                            }
                                            if (track.durationMs > 0L) {
                                                Text(
                                                    text = formatWebMs(track.durationMs),
                                                    style = MaterialTheme.typography.labelMedium,
                                                    color = cs.onSurfaceVariant,
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // ─── 3. JOIN / SWITCH CODE & PLATFORM GUIDE ISLAND ────────────────────────────
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(36.dp),
                color = cs.surfaceContainerLow,
            ) {
                Column(
                    modifier = Modifier.padding(if (isMobile) 20.dp else 36.dp),
                    verticalArrangement = Arrangement.spacedBy(28.dp),
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                    ) {
                        WebExpressiveCookieBadge(
                            isActive = normalizedCode == null,
                            size = 56.dp,
                            activeColor = cs.primary,
                            inactiveColor = cs.secondaryContainer,
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Podcasts,
                                contentDescription = null,
                                tint = if (normalizedCode == null) cs.onPrimary else cs.onSecondaryContainer,
                                modifier = Modifier.size(26.dp),
                            )
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = if (normalizedCode == null) {
                                    if (isFr) "Écouter en direct avec un ami" else "Listen Along Live with Friends"
                                } else {
                                    if (isFr) "Rejoindre un autre code ou salon" else "Open Another Session Code"
                                },
                                style = if (isMobile) MaterialTheme.typography.titleLarge else MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.ExtraBold,
                                color = cs.onSurface,
                            )
                            Text(
                                text = if (isFr) {
                                    "Collez un lien d'invitation KittyTune ou un code à 8 caractères (ex. ABCD-EFGH) pour voir la musique en direct et rejoindre la session."
                                } else {
                                    "Paste a KittyTune invite link or 8-character code (e.g. ABCD-EFGH) to preview what's playing live and join the session."
                                },
                                style = MaterialTheme.typography.bodyMedium,
                                color = cs.onSurfaceVariant,
                            )
                        }
                    }

                    val submitCode = {
                        TogetherWebLinks.extractUserInputCode(codeInput)?.let { parsed ->
                            codeInput = TogetherWebLinks.displayCode(parsed)
                            onCodeSelected(parsed)
                        }
                    }

                    if (isMobile) {
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            OutlinedTextField(
                                value = codeInput,
                                onValueChange = { codeInput = it },
                                placeholder = {
                                    Text(if (isFr) "Code ABCD-EFGH ou lien d'invitation" else "Code ABCD-EFGH or invite URL")
                                },
                                leadingIcon = { Icon(Icons.Filled.Link, contentDescription = null) },
                                singleLine = true,
                                shape = RoundedCornerShape(18.dp),
                                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Go),
                                keyboardActions = KeyboardActions(onGo = { submitCode() }),
                                modifier = Modifier.fillMaxWidth(),
                            )
                            Button(
                                onClick = { submitCode() },
                                enabled = TogetherWebLinks.extractUserInputCode(codeInput) != null,
                                shapes = ButtonDefaults.shapes(),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(52.dp),
                            ) {
                                Icon(Icons.Filled.Headphones, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    text = if (isFr) "Voir la session en direct" else "Preview Live Session",
                                    fontWeight = FontWeight.Bold,
                                )
                            }
                        }
                    } else {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            OutlinedTextField(
                                value = codeInput,
                                onValueChange = { codeInput = it },
                                placeholder = {
                                    Text(if (isFr) "Code ABCD-EFGH ou lien d'invitation" else "Code ABCD-EFGH or invite URL")
                                },
                                leadingIcon = { Icon(Icons.Filled.Link, contentDescription = null) },
                                singleLine = true,
                                shape = RoundedCornerShape(18.dp),
                                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Go),
                                keyboardActions = KeyboardActions(onGo = { submitCode() }),
                                modifier = Modifier.weight(1f),
                            )
                            Button(
                                onClick = { submitCode() },
                                enabled = TogetherWebLinks.extractUserInputCode(codeInput) != null,
                                shapes = ButtonDefaults.shapes(),
                                modifier = Modifier.height(56.dp),
                                contentPadding = PaddingValues(horizontal = 28.dp),
                            ) {
                                Icon(Icons.Filled.Headphones, contentDescription = null, modifier = Modifier.size(20.dp))
                                Spacer(Modifier.width(10.dp))
                                Text(
                                    text = if (isFr) "Voir la session en direct" else "Preview Live Session",
                                    fontWeight = FontWeight.Bold,
                                )
                            }
                        }
                    }

                    // Platform How-To Cards (Android + Desktop)
                    AdaptivePair(
                        spacing = 20.dp,
                        first = { mod ->
                            PlatformJoinCard(
                                modifier = mod,
                                icon = Icons.Filled.PhoneAndroid,
                                title = "KittyTune Android",
                                subtitle = if (isFr) {
                                    "Cliquez sur « Rejoindre la session » ci-dessus pour ouvrir directement KittyTune sur votre téléphone et synchroniser la lecture."
                                } else {
                                    "Tap 'Join Session' above to launch KittyTune directly on your Android phone and sync playback automatically."
                                },
                                actionLabel = if (isFr) "Télécharger l'APK Android" else "Download Android APK",
                                onAction = onDownloadClick,
                            )
                        },
                        second = { mod ->
                            PlatformJoinCard(
                                modifier = mod,
                                icon = Icons.Filled.DesktopWindows,
                                title = "KittyTune Desktop",
                                subtitle = if (isFr) {
                                    "Sur Windows, Linux ou macOS : cliquez sur « Ouvrir KittyTune » ci-dessus pour lancer directement l'application sur la session, ou collez le code dans l'onglet Together."
                                } else {
                                    "On Windows, Linux, or macOS: click 'Open KittyTune' above to launch the desktop app directly into the session, or paste the code in the Together tab."
                                },
                                actionLabel = if (isFr) "Télécharger pour PC / Mac / Linux" else "Download for Desktop",
                                onAction = onDownloadClick,
                            )
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun InvitePromptColumn(
    isFr: Boolean,
    isMobile: Boolean,
    isLive: Boolean,
    hostName: String?,
    formattedCode: String,
    session: TogetherWebSessionState,
    copiedCode: Boolean,
    copiedLink: Boolean,
    joinAttempted: Boolean,
    onJoinInApp: () -> Unit,
    onCopyCode: () -> Unit,
    onCopyLink: () -> Unit,
    onDownloadClick: () -> Unit,
) {
    val cs = MaterialTheme.colorScheme

    Column(verticalArrangement = Arrangement.spacedBy(20.dp)) {
        // Live Status + Mode Badges Row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Surface(
                shape = CircleShape,
                color = when {
                    isLive -> cs.primaryContainer
                    session.connection == WebRelayConnection.CONNECTING -> cs.secondaryContainer
                    else -> cs.surfaceContainerHighest
                },
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    if (isLive && session.isPlaying) {
                        WebListeningBars(
                            isPlaying = true,
                            color = cs.onPrimaryContainer,
                            modifier = Modifier.size(16.dp),
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Filled.Podcasts,
                            contentDescription = null,
                            tint = if (isLive) cs.onPrimaryContainer else cs.onSecondaryContainer,
                            modifier = Modifier.size(16.dp),
                        )
                    }
                    Text(
                        text = when {
                            isLive -> if (isFr) "EN DIRECT · SYNCHRONISÉ" else "LIVE · IN SYNC"
                            session.connection == WebRelayConnection.CONNECTING ->
                                if (isFr) "CONNEXION AU SALON…" else "CONNECTING TO SESSION…"
                            else -> if (isFr) "EN ATTENTE DE L'HÔTE" else "WAITING FOR HOST"
                        },
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = when {
                            isLive -> cs.onPrimaryContainer
                            session.connection == WebRelayConnection.CONNECTING -> cs.onSecondaryContainer
                            else -> cs.onSurfaceVariant
                        },
                    )
                }
            }

            if (isLive) {
                Surface(
                    shape = CircleShape,
                    color = cs.tertiaryContainer,
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        Icon(
                            imageVector = if (session.hostLocked) Icons.Filled.Lock else Icons.Filled.LockOpen,
                            contentDescription = null,
                            tint = cs.onTertiaryContainer,
                            modifier = Modifier.size(15.dp),
                        )
                        Text(
                            text = if (session.hostLocked) {
                                if (isFr) "Écoute seule" else "Listen-only"
                            } else {
                                if (isFr) "Session ouverte" else "Interactive"
                            },
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = cs.onTertiaryContainer,
                        )
                    }
                }
            }
        }

        // Main Invite Question Headline
        Text(
            text = when {
                !hostName.isNullOrBlank() && isFr -> "Voulez-vous écouter en direct avec $hostName ?"
                !hostName.isNullOrBlank() -> "Want to listen along live with $hostName?"
                isFr -> "Voulez-vous rejoindre cette session d'écoute ?"
                else -> "Want to join this live listening session?"
            },
            style = if (isMobile) MaterialTheme.typography.headlineLarge else MaterialTheme.typography.displaySmall,
            fontWeight = FontWeight.Black,
            color = cs.onSurface,
            lineHeight = if (isMobile) 38.sp else 48.sp,
        )

        Text(
            text = if (!hostName.isNullOrBlank()) {
                if (isFr) {
                    "$hostName partage sa musique en direct sur KittyTune. Rejoignez sa session pour écouter exactement les mêmes morceaux au même moment, sans décalage."
                } else {
                    "$hostName is broadcasting their music live on KittyTune. Join their session to hear the exact same tracks at the exact same moment."
                }
            } else {
                if (isFr) {
                    "Rejoignez ce salon KittyTune Together ($formattedCode) pour écouter la même musique en direct et en parfaite synchronisation sur Android ou PC."
                } else {
                    "Join this KittyTune Together room ($formattedCode) to listen to the exact same music live and in sync on Android or Desktop."
                }
            },
            style = MaterialTheme.typography.bodyLarge,
            color = cs.onSurfaceVariant,
            lineHeight = 26.sp,
        )

        // Primary Join Action Card
        Surface(
            shape = RoundedCornerShape(28.dp),
            color = cs.surfaceContainer,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(
                modifier = Modifier.padding(if (isMobile) 16.dp else 20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Button(
                    onClick = onJoinInApp,
                    shapes = ButtonDefaults.shapes(),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(58.dp),
                ) {
                    Icon(Icons.Filled.Headphones, contentDescription = null, modifier = Modifier.size(22.dp))
                    Spacer(Modifier.width(10.dp))
                    Text(
                        text = if (!hostName.isNullOrBlank()) {
                            if (isFr) "Écouter avec $hostName · Ouvrir KittyTune" else "Listen with $hostName · Open KittyTune"
                        } else {
                            if (isFr) "Écouter et ouvrir KittyTune" else "Listen Along · Open KittyTune"
                        },
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.ExtraBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }

                AnimatedVisibility(
                    visible = joinAttempted,
                    enter = fadeIn() + expandVertically(),
                    exit = fadeOut() + shrinkVertically(),
                ) {
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = cs.secondaryContainer,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            Text(
                                text = if (isFr) {
                                    "Cliquez sur « Ouvrir » dans la fenêtre de votre navigateur pour lancer KittyTune (Android ou PC/Mac/Linux) et rejoindre la session automatiquement. Vous n'avez pas encore KittyTune ?"
                                } else {
                                    "Click 'Open' in your browser's prompt to launch KittyTune (Android or Desktop) and join the live session automatically. Don't have KittyTune installed yet?"
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = cs.onSecondaryContainer,
                            )
                            Button(
                                onClick = onDownloadClick,
                                shapes = ButtonDefaults.shapes(),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(46.dp),
                            ) {
                                Icon(Icons.Filled.Download, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    text = if (isFr) "Télécharger l'application KittyTune" else "Download KittyTune App",
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                )
                            }
                        }
                    }
                }

                // Secondary Action Buttons Row
                if (isMobile) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilledTonalButton(
                            onClick = onCopyCode,
                            shapes = ButtonDefaults.shapes(),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp),
                        ) {
                            Icon(
                                if (copiedCode) Icons.Filled.Check else Icons.Filled.ContentCopy,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp),
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = if (copiedCode) {
                                    if (isFr) "Code $formattedCode copié !" else "Copied $formattedCode!"
                                } else {
                                    if (isFr) "Copier le code ($formattedCode)" else "Copy Code ($formattedCode)"
                                },
                                fontWeight = FontWeight.Bold,
                            )
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                            OutlinedButton(
                                onClick = onCopyLink,
                                shapes = ButtonDefaults.shapes(),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp),
                            ) {
                                Icon(
                                    if (copiedLink) Icons.Filled.Check else Icons.Filled.Share,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp),
                                )
                                Spacer(Modifier.width(6.dp))
                                Text(
                                    text = if (copiedLink) {
                                        if (isFr) "Copié !" else "Copied!"
                                    } else {
                                        if (isFr) "Lien" else "Share"
                                    },
                                    maxLines = 1,
                                )
                            }
                            OutlinedButton(
                                onClick = onDownloadClick,
                                shapes = ButtonDefaults.shapes(),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp),
                            ) {
                                Icon(Icons.Filled.Download, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(6.dp))
                                Text(
                                    text = if (isFr) "Installer" else "Get App",
                                    maxLines = 1,
                                )
                            }
                        }
                    }
                } else {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        FilledTonalButton(
                            onClick = onCopyCode,
                            shapes = ButtonDefaults.shapes(),
                            modifier = Modifier
                                .weight(1.2f)
                                .height(48.dp),
                        ) {
                            Icon(
                                if (copiedCode) Icons.Filled.Check else Icons.Filled.ContentCopy,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp),
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = if (copiedCode) {
                                    if (isFr) "Code copié !" else "Code copied!"
                                } else {
                                    "Code $formattedCode"
                                },
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                            )
                        }
                        OutlinedButton(
                            onClick = onCopyLink,
                            shapes = ButtonDefaults.shapes(),
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp),
                        ) {
                            Icon(
                                if (copiedLink) Icons.Filled.Check else Icons.Filled.Share,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp),
                            )
                            Spacer(Modifier.width(6.dp))
                            Text(
                                text = if (copiedLink) {
                                    if (isFr) "Lien copié !" else "Link copied!"
                                } else {
                                    if (isFr) "Copier le lien" else "Copy Link"
                                },
                                maxLines = 1,
                            )
                        }
                        OutlinedButton(
                            onClick = onDownloadClick,
                            shapes = ButtonDefaults.shapes(),
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp),
                        ) {
                            Icon(Icons.Filled.Download, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text(
                                text = if (isFr) "Télécharger" else "Download",
                                maxLines = 1,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun LivePlayerShowcaseCard(
    isFr: Boolean,
    isMobile: Boolean,
    isLive: Boolean,
    session: TogetherWebSessionState,
    listeners: List<WebMember>,
    nowMs: Long,
) {
    val cs = MaterialTheme.colorScheme
    val track = session.nowPlaying

    Surface(
        shape = RoundedCornerShape(32.dp),
        color = cs.surfaceContainer,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier.padding(if (isMobile) 18.dp else 24.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            // Card Header: Scalloped Cookie + Session Room Name
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                WebExpressiveCookieBadge(
                    isActive = isLive && session.isPlaying,
                    size = 44.dp,
                    activeColor = cs.primary,
                    inactiveColor = cs.secondaryContainer,
                ) {
                    Icon(
                        imageVector = if (session.isPlaying) Icons.Filled.GraphicEq else Icons.Filled.MusicNote,
                        contentDescription = null,
                        tint = if (isLive && session.isPlaying) cs.onPrimary else cs.onSecondaryContainer,
                        modifier = Modifier.size(22.dp),
                    )
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = if (isFr) "EN COURS DANS LA SESSION" else "NOW PLAYING IN SESSION",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.ExtraBold,
                        color = cs.primary,
                    )
                    Text(
                        text = session.roomName.ifBlank { "Together · ${TogetherWebLinks.displayCode(session.code)}" },
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = cs.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }

            if (track != null) {
                // Live Track Artwork & Metadata
                Surface(
                    shape = RoundedCornerShape(24.dp),
                    color = cs.surfaceContainerHigh,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(16.dp),
                        ) {
                            WebTrackArtwork(
                                url = track.artworkUrl,
                                title = track.title,
                                size = if (isMobile) 84.dp else 104.dp,
                                corner = 20.dp,
                            )
                            Column(
                                modifier = Modifier.weight(1f),
                                verticalArrangement = Arrangement.spacedBy(6.dp),
                            ) {
                                track.source?.takeIf { it.isNotBlank() }?.let { src ->
                                    Surface(
                                        shape = CircleShape,
                                        color = cs.secondaryContainer,
                                    ) {
                                        Text(
                                            text = src.replaceFirstChar { it.uppercase() },
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = cs.onSecondaryContainer,
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp),
                                        )
                                    }
                                }
                                Text(
                                    text = track.title.ifBlank { if (isFr) "Titre en cours" else "Playing track" },
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = cs.onSurface,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis,
                                )
                                Text(
                                    text = track.artist.ifBlank { "KittyTune" },
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = cs.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }
                        }

                        if (track.durationMs > 0L) {
                            WebSineProgressBar(
                                currentMs = session.interpolatedPositionMs(nowMs),
                                durationMs = track.durationMs,
                                isPlaying = session.isPlaying,
                            )
                        }
                    }
                }
            } else {
                // Waiting / Connecting State Card
                Surface(
                    shape = RoundedCornerShape(24.dp),
                    color = cs.surfaceContainerHigh,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        WebExpressiveCookieBadge(
                            isActive = session.connection == WebRelayConnection.CONNECTING,
                            size = 72.dp,
                            activeColor = cs.primaryContainer,
                            inactiveColor = cs.secondaryContainer,
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Headphones,
                                contentDescription = null,
                                tint = if (session.connection == WebRelayConnection.CONNECTING) {
                                    cs.onPrimaryContainer
                                } else {
                                    cs.onSecondaryContainer
                                },
                                modifier = Modifier.size(32.dp),
                            )
                        }
                        Text(
                            text = when (session.connection) {
                                WebRelayConnection.CONNECTING ->
                                    if (isFr) "Synchronisation avec l'hôte en cours…" else "Syncing with live host…"
                                WebRelayConnection.OFFLINE ->
                                    if (isFr) "Relais hors ligne — reconnexion…" else "Relay offline — reconnecting…"
                                else ->
                                    if (isFr) "En attente du prochain morceau en direct" else "Waiting for the host to play a track"
                            },
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = cs.onSurface,
                        )
                        Text(
                            text = if (isFr) {
                                "Dès que la personne lance ou change un son dans KittyTune, la pochette et la progression s'affichent ici instantanément."
                            } else {
                                "As soon as the host plays or switches a song in KittyTune, the artwork and live progress appear here automatically."
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = cs.onSurfaceVariant,
                        )
                    }
                }
            }

            // Listeners Strip
            val displayListeners = remember(listeners, session.hostId, session.hostName) {
                if (listeners.isNotEmpty()) {
                    listeners
                } else if (!session.hostName.isNullOrBlank() && session.hostId != null) {
                    listOf(WebMember(session.hostId, session.hostName, true, nowMs))
                } else {
                    emptyList()
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = if (displayListeners.isNotEmpty()) {
                        if (isFr) "À l'écoute en ce moment (${displayListeners.size})" else "Listening right now (${displayListeners.size})"
                    } else {
                        if (isFr) "Participants" else "Listeners"
                    },
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = cs.onSurfaceVariant,
                )

                if (displayListeners.isNotEmpty()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        displayListeners.forEach { member ->
                            val isHostMember = member.id == session.hostId
                            Surface(
                                shape = CircleShape,
                                color = if (isHostMember) cs.primaryContainer else cs.surfaceContainerHighest,
                            ) {
                                Row(
                                    modifier = Modifier.padding(start = 6.dp, end = 12.dp, top = 6.dp, bottom = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(24.dp)
                                            .clip(CircleShape)
                                            .background(if (isHostMember) cs.primary else cs.secondaryContainer),
                                        contentAlignment = Alignment.Center,
                                    ) {
                                        Text(
                                            text = member.name.trim().take(1).uppercase().ifBlank { "?" },
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = if (isHostMember) cs.onPrimary else cs.onSecondaryContainer,
                                        )
                                    }
                                    Text(
                                        text = member.name,
                                        style = MaterialTheme.typography.labelLarge,
                                        fontWeight = if (isHostMember) FontWeight.ExtraBold else FontWeight.SemiBold,
                                        color = if (isHostMember) cs.onPrimaryContainer else cs.onSurface,
                                    )
                                    if (isHostMember) {
                                        Icon(
                                            imageVector = Icons.Filled.Star,
                                            contentDescription = null,
                                            tint = cs.primary,
                                            modifier = Modifier.size(14.dp),
                                        )
                                    }
                                }
                            }
                        }
                    }
                } else {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Group,
                            contentDescription = null,
                            tint = cs.onSurfaceVariant,
                            modifier = Modifier.size(18.dp),
                        )
                        Text(
                            text = if (isFr) "Soyez le premier à rejoindre ce salon" else "Be the first to join this room",
                            style = MaterialTheme.typography.bodySmall,
                            color = cs.onSurfaceVariant,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PlatformJoinCard(
    modifier: Modifier,
    icon: ImageVector,
    title: String,
    subtitle: String,
    actionLabel: String,
    onAction: () -> Unit,
) {
    val cs = MaterialTheme.colorScheme
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(26.dp),
        color = cs.surfaceContainer,
    ) {
        Column(
            modifier = Modifier.padding(22.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(cs.secondaryContainer),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(icon, contentDescription = null, tint = cs.onSecondaryContainer, modifier = Modifier.size(22.dp))
                }
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = cs.onSurface,
                )
            }
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = cs.onSurfaceVariant,
                lineHeight = 22.sp,
            )
            FilledTonalButton(
                onClick = onAction,
                shapes = ButtonDefaults.shapes(),
            ) {
                Icon(Icons.Filled.Download, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text(actionLabel, fontWeight = FontWeight.Bold)
            }
        }
    }
}

// ─── Web M3 Expressive Primitives (Scalloped Cookie, Spring Group Item, Sine Progress) ──────

@Composable
private fun WebExpressiveCookieBadge(
    isActive: Boolean,
    size: Dp,
    activeColor: Color,
    inactiveColor: Color,
    content: @Composable () -> Unit,
) {
    val bg by animateColorAsState(
        targetValue = if (isActive) activeColor else inactiveColor,
        animationSpec = tween(260),
    )
    val inf = rememberInfiniteTransition()
    val spin by inf.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(16000, easing = LinearEasing), RepeatMode.Restart),
    )
    val rest by animateFloatAsState(
        targetValue = if (isActive) 20f else 0f,
        animationSpec = spring(dampingRatio = 0.55f, stiffness = Spring.StiffnessLow),
    )

    Box(
        modifier = Modifier
            .size(size)
            .drawWithCache {
                val path = Path()
                val waves = 12
                val amp = 0.065f
                val steps = 240
                val cx = this.size.width / 2f
                val cy = this.size.height / 2f
                val r = min(this.size.width, this.size.height) / 2f
                for (i in 0 until steps) {
                    val t = i.toDouble() / steps * 2.0 * PI
                    val k = 1f - amp + amp * cos(waves * t).toFloat()
                    val a = t - PI / 2.0
                    val x = cx + r * k * cos(a).toFloat()
                    val y = cy + r * k * sin(a).toFloat()
                    if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
                }
                path.close()
                onDrawBehind {
                    rotate(if (isActive) spin else rest, center) {
                        drawPath(path, bg)
                    }
                }
            },
        contentAlignment = Alignment.Center,
    ) {
        content()
    }
}

@Composable
private fun WebExpressiveGroupRow(
    onClick: (() -> Unit)? = null,
    content: @Composable () -> Unit,
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val corner by animateDpAsState(
        targetValue = if (pressed) 22.dp else 6.dp,
        animationSpec = spring(dampingRatio = 0.6f, stiffness = 700f),
    )
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(corner))
            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
            .then(
                if (onClick != null) {
                    Modifier.clickable(
                        interactionSource = interaction,
                        indication = null,
                        onClick = onClick,
                    )
                } else {
                    Modifier
                }
            ),
    ) {
        content()
    }
}

@Composable
private fun WebTrackArtwork(
    url: String?,
    title: String,
    size: Dp,
    corner: Dp,
) {
    val cs = MaterialTheme.colorScheme
    Box(
        modifier = Modifier
            .size(size)
            .clip(RoundedCornerShape(corner))
            .background(cs.secondaryContainer),
        contentAlignment = Alignment.Center,
    ) {
        if (!url.isNullOrBlank()) {
            HtmlImage(
                src = url,
                description = title,
                corner = corner,
                fit = MediaFit.Cover,
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(corner)),
            )
        } else {
            Icon(
                imageVector = Icons.Filled.MusicNote,
                contentDescription = null,
                tint = cs.onSecondaryContainer,
                modifier = Modifier.size(size * 0.45f),
            )
        }
    }
}

@Composable
private fun WebListeningBars(
    isPlaying: Boolean,
    color: Color,
    modifier: Modifier = Modifier,
) {
    val inf = rememberInfiniteTransition()
    val phase by inf.animateFloat(
        initialValue = 0f,
        targetValue = if (isPlaying) 1f else 0f,
        animationSpec = infiniteRepeatable(tween(650), RepeatMode.Restart),
    )
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(2.5.dp),
        verticalAlignment = Alignment.Bottom,
    ) {
        listOf(0f, 0.33f, 0.66f).forEach { off ->
            val h = if (isPlaying) {
                0.35f + 0.65f * (0.5f + 0.5f * sin((phase + off) * 2f * PI.toFloat()))
            } else {
                0.35f
            }
            Box(
                Modifier
                    .width(3.dp)
                    .height((16 * h).dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(color),
            )
        }
    }
}

@Composable
private fun WebSineProgressBar(
    currentMs: Long,
    durationMs: Long,
    isPlaying: Boolean,
) {
    val fraction = (currentMs.toFloat() / durationMs.coerceAtLeast(1L).toFloat()).coerceIn(0f, 1f)
    val activeColor = MaterialTheme.colorScheme.primary
    val inactiveColor = MaterialTheme.colorScheme.surfaceContainerHighest
    val density = LocalDensity.current

    val inf = rememberInfiniteTransition()
    val phase by inf.animateFloat(
        initialValue = 0f,
        targetValue = 2f * PI.toFloat(),
        animationSpec = infiniteRepeatable(tween(1800, easing = LinearEasing), RepeatMode.Restart),
    )
    val ampScale by animateFloatAsState(
        targetValue = if (isPlaying) 1f else 0f,
        animationSpec = tween(250),
    )

    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(18.dp),
        ) {
            val w = size.width
            val cy = size.height / 2f
            val strokePx = with(density) { 4.dp.toPx() }
            val ampPx = with(density) { 3.5.dp.toPx() } * ampScale
            val waveLenPx = with(density) { 28.dp.toPx() }
            val splitX = (w * fraction).coerceIn(0f, w)

            if (splitX < w) {
                drawLine(
                    color = inactiveColor,
                    start = Offset((splitX + strokePx * 1.4f).coerceAtMost(w), cy),
                    end = Offset(w, cy),
                    strokeWidth = strokePx,
                    cap = StrokeCap.Round,
                )
            }

            if (splitX > 1f) {
                if (ampPx > 0.2f) {
                    val steps = (splitX / 2.5f).toInt().coerceIn(8, 280)
                    val path = Path().apply {
                        for (i in 0..steps) {
                            val x = splitX * (i.toFloat() / steps)
                            val theta = (x / waveLenPx) * (2f * PI.toFloat()) + phase
                            val y = cy + ampPx * sin(theta)
                            if (i == 0) moveTo(x, y) else lineTo(x, y)
                        }
                    }
                    drawPath(
                        path = path,
                        color = activeColor,
                        style = Stroke(width = strokePx, cap = StrokeCap.Round, join = StrokeJoin.Round),
                    )
                } else {
                    drawLine(
                        color = activeColor,
                        start = Offset(0f, cy),
                        end = Offset(splitX, cy),
                        strokeWidth = strokePx,
                        cap = StrokeCap.Round,
                    )
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = formatWebMs(currentMs),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = formatWebMs(durationMs),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

private fun formatWebMs(ms: Long): String {
    val totalSec = (ms / 1000L).coerceAtLeast(0L)
    val min = totalSec / 60L
    val sec = (totalSec % 60L).toString().padStart(2, '0')
    return "$min:$sec"
}
