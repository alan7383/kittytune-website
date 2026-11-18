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
import androidx.compose.foundation.Image
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
import androidx.compose.material.icons.filled.Podcasts
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
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.toComposeImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
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

private val avatarBitmapCache = mutableMapOf<String, ImageBitmap>()

@Composable
fun ListenAlongPage(
    activeCode: String?,
    onCodeSelected: (String) -> Unit,
    onDownloadClick: () -> Unit,
) {
    val locale = LocalWebLocale.current
    val s = remember(locale) { listenAlongStrings(locale) }
    val normalizedCode = remember(activeCode) {
        activeCode?.let(TogetherWebLinks::normalizeCode)?.takeIf { it.length == TogetherWebLinks.CODE_LENGTH }
    }

    var session by remember(normalizedCode) {
        mutableStateOf(
            TogetherWebSessionState(
                code = normalizedCode.orEmpty(),
                connection = if (normalizedCode != null) WebRelayConnection.CONNECTING else WebRelayConnection.IDLE,
            )
        )
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

    // Safety fallback so initial sync never hangs indefinitely if a broker delays SUBACK
    LaunchedEffect(normalizedCode, session.connection, session.initialSyncComplete) {
        if (normalizedCode != null && !session.initialSyncComplete && session.connection != WebRelayConnection.OFFLINE) {
            delay(3000L)
            session = session.copy(initialSyncComplete = true)
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
            // Top Utility Row: End-to-End Encryption Pill (global 8-language selector lives in App.kt navbar)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Start,
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
                            text = s.e2eeBadge,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = cs.onSurfaceVariant,
                        )
                    }
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
                                s = s,
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
                                s = s,
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
                                    s = s,
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
                                    s = s,
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
                if (isLive && (session.upcoming.isNotEmpty() || session.playlist.isNotEmpty())) {
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
                                    text = s.liveQueueTitle,
                                    style = if (isMobile) MaterialTheme.typography.titleLarge else MaterialTheme.typography.headlineSmall,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = cs.onSurface,
                                )

                                if (session.upcoming.isNotEmpty() && session.playlist.isNotEmpty()) {
                                    Row(
                                        modifier = Modifier
                                            .widthIn(max = 360.dp)
                                            .clip(RoundedCornerShape(20.dp))
                                            .background(cs.surfaceContainer)
                                            .padding(4.dp),
                                        horizontalArrangement = Arrangement.spacedBy(ButtonGroupDefaults.ConnectedSpaceBetween),
                                    ) {
                                        val opts = listOf(
                                            QueueViewTab.UPCOMING to s.upNextTab(session.upcoming.size),
                                            QueueViewTab.PLAYLIST to s.playlistTab(session.playlist.size),
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
                                                Text(label, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, maxLines = 1)
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
                                                    text = track.title.ifBlank { s.untitledTrack },
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
                                text = if (normalizedCode == null) s.joinHeroTitle else s.switchSessionTitle,
                                style = if (isMobile) MaterialTheme.typography.titleLarge else MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.ExtraBold,
                                color = cs.onSurface,
                            )
                            Text(
                                text = s.joinHeroSubtitle,
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
                                    Text(s.codeInputPlaceholder)
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
                                    text = s.previewSessionButton,
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
                                    Text(s.codeInputPlaceholder)
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
                                    text = s.previewSessionButton,
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
                                title = s.androidCardTitle,
                                subtitle = s.androidCardSubtitle,
                                actionLabel = s.androidCardAction,
                                onAction = onDownloadClick,
                            )
                        },
                        second = { mod ->
                            PlatformJoinCard(
                                modifier = mod,
                                icon = Icons.Filled.DesktopWindows,
                                title = s.desktopCardTitle,
                                subtitle = s.desktopCardSubtitle,
                                actionLabel = s.desktopCardAction,
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
    s: ListenAlongStrings,
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
    val isSyncingRoom = !isLive && !session.initialSyncComplete && session.connection != WebRelayConnection.OFFLINE

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
                    isSyncingRoom -> cs.secondaryContainer
                    else -> cs.surfaceContainerHighest
                },
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    when {
                        isLive && session.isPlaying -> {
                            WebListeningBars(
                                isPlaying = true,
                                color = cs.onPrimaryContainer,
                                modifier = Modifier.size(16.dp),
                            )
                        }
                        isSyncingRoom -> {
                            WebListeningBars(
                                isPlaying = true,
                                color = cs.onSecondaryContainer,
                                modifier = Modifier.size(16.dp),
                            )
                        }
                        else -> {
                            Icon(
                                imageVector = Icons.Filled.Podcasts,
                                contentDescription = null,
                                tint = if (isLive) cs.onPrimaryContainer else cs.onSurfaceVariant,
                                modifier = Modifier.size(16.dp),
                            )
                        }
                    }
                    Text(
                        text = when {
                            isLive -> s.statusLiveInSync
                            session.connection == WebRelayConnection.OFFLINE -> s.relayOfflineReconnecting
                            !session.initialSyncComplete -> s.statusSyncingSession
                            else -> s.statusWaitingForHost
                        },
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = when {
                            isLive -> cs.onPrimaryContainer
                            isSyncingRoom -> cs.onSecondaryContainer
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
                            text = if (session.hostLocked) s.modeListenOnly else s.modeInteractive,
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
            text = if (!hostName.isNullOrBlank()) {
                s.inviteHeadlineWithHost(hostName)
            } else {
                s.inviteHeadlineGeneric
            },
            style = if (isMobile) MaterialTheme.typography.headlineLarge else MaterialTheme.typography.displaySmall,
            fontWeight = FontWeight.Black,
            color = cs.onSurface,
            lineHeight = if (isMobile) 38.sp else 48.sp,
        )

        Text(
            text = if (!hostName.isNullOrBlank()) {
                s.inviteBodyWithHost(hostName)
            } else {
                s.inviteBodyGeneric(formattedCode)
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
                            s.joinButtonWithHost(hostName)
                        } else {
                            s.joinButtonGeneric
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
                                text = s.joinPromptHint,
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
                                    text = s.downloadAppButton,
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
                                text = if (copiedCode) s.mobileCopiedCode(formattedCode) else s.mobileCopyCode(formattedCode),
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
                                    text = if (copiedLink) s.mobileCopiedLink else s.mobileShareLink,
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
                                    text = s.mobileGetApp,
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
                                text = if (copiedCode) s.desktopCopiedCode else s.desktopCodeLabel(formattedCode),
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
                                text = if (copiedLink) s.desktopCopiedLink else s.desktopCopyLink,
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
                                text = s.desktopDownload,
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
    s: ListenAlongStrings,
    isMobile: Boolean,
    isLive: Boolean,
    session: TogetherWebSessionState,
    listeners: List<WebMember>,
    nowMs: Long,
) {
    val cs = MaterialTheme.colorScheme
    val track = session.nowPlaying
    val isSyncingRoom = !isLive && !session.initialSyncComplete && session.connection != WebRelayConnection.OFFLINE

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
                    isActive = (isLive && session.isPlaying) || isSyncingRoom,
                    size = 44.dp,
                    activeColor = if (isLive && session.isPlaying) cs.primary else cs.secondaryContainer,
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
                        text = s.nowPlayingInSession,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.ExtraBold,
                        color = cs.primary,
                    )
                    Text(
                        text = session.roomName.ifBlank { s.defaultRoomName(TogetherWebLinks.displayCode(session.code)) },
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
                                    text = track.title.ifBlank { s.playingTrackFallback },
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
                // Syncing / Waiting State Card
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
                            isActive = isSyncingRoom,
                            size = 72.dp,
                            activeColor = cs.primaryContainer,
                            inactiveColor = cs.secondaryContainer,
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Headphones,
                                contentDescription = null,
                                tint = if (isSyncingRoom) {
                                    cs.onPrimaryContainer
                                } else {
                                    cs.onSecondaryContainer
                                },
                                modifier = Modifier.size(32.dp),
                            )
                        }
                        Text(
                            text = when {
                                session.connection == WebRelayConnection.OFFLINE -> s.relayOfflineReconnecting
                                !session.initialSyncComplete -> s.syncingLiveTitle
                                else -> s.waitingForTrack
                            },
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = cs.onSurface,
                            textAlign = TextAlign.Center,
                        )
                        Text(
                            text = if (!session.initialSyncComplete && session.connection != WebRelayConnection.OFFLINE) {
                                s.syncingLiveSubtitle
                            } else {
                                s.waitingForTrackHint
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = cs.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                        )
                    }
                }
            }

            // Listeners Strip
            val displayListeners = remember(listeners, session.hostId, session.hostName, session.hostAvatarUrl) {
                if (listeners.isNotEmpty()) {
                    listeners.map { member ->
                        if (member.id == session.hostId && member.avatarUrl == null && session.hostAvatarUrl != null) {
                            member.copy(avatarUrl = session.hostAvatarUrl)
                        } else {
                            member
                        }
                    }
                } else if (!session.hostName.isNullOrBlank() && session.hostId != null) {
                    listOf(WebMember(session.hostId, session.hostName, true, nowMs, session.hostAvatarUrl))
                } else {
                    emptyList()
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = if (displayListeners.isNotEmpty()) {
                        s.listeningRightNow(displayListeners.size)
                    } else {
                        s.listenersHeader
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
                                    modifier = Modifier.padding(start = 5.dp, end = 12.dp, top = 5.dp, bottom = 5.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                ) {
                                    WebMemberAvatar(
                                        name = member.name,
                                        avatarUrl = member.avatarUrl,
                                        isHost = isHostMember,
                                        size = 28.dp,
                                    )
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
                } else if (!session.initialSyncComplete) {
                    WebSyncingListenersPlaceholder(
                        label = if (session.connection == WebRelayConnection.OFFLINE) {
                            s.relayOfflineReconnecting
                        } else {
                            s.findingConnectedListeners
                        },
                    )
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
                            text = s.beFirstToJoin,
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
private fun WebSyncingListenersPlaceholder(
    label: String,
) {
    val cs = MaterialTheme.colorScheme
    val inf = rememberInfiniteTransition()
    val pulseA by inf.animateFloat(
        initialValue = 0.45f,
        targetValue = 0.92f,
        animationSpec = infiniteRepeatable(tween(820, easing = LinearEasing), RepeatMode.Reverse),
    )
    val pulseB by inf.animateFloat(
        initialValue = 0.92f,
        targetValue = 0.45f,
        animationSpec = infiniteRepeatable(tween(820, easing = LinearEasing), RepeatMode.Reverse),
    )

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            listOf(76.dp to pulseA, 58.dp to pulseB).forEach { (barWidth, alpha) ->
                Surface(
                    shape = CircleShape,
                    color = cs.surfaceContainerHighest.copy(alpha = alpha),
                ) {
                    Row(
                        modifier = Modifier.padding(start = 5.dp, end = 14.dp, top = 5.dp, bottom = 5.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .clip(CircleShape)
                                .background(cs.secondaryContainer.copy(alpha = alpha)),
                        )
                        Box(
                            modifier = Modifier
                                .width(barWidth)
                                .height(10.dp)
                                .clip(CircleShape)
                                .background(cs.onSurfaceVariant.copy(alpha = 0.24f * alpha)),
                        )
                    }
                }
            }
        }
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = cs.onSurfaceVariant,
        )
    }
}

@Composable
private fun WebMemberAvatar(
    name: String,
    avatarUrl: String?,
    isHost: Boolean,
    size: Dp = 28.dp,
) {
    val cs = MaterialTheme.colorScheme
    val validUrl = remember(avatarUrl) { sanitizeTogetherAvatarUrl(avatarUrl) }
    var bitmap by remember(validUrl) {
        mutableStateOf(validUrl?.let { avatarBitmapCache[it] })
    }

    DisposableEffect(validUrl) {
        var disposed = false
        if (validUrl != null) {
            val cached = avatarBitmapCache[validUrl]
            if (cached != null) {
                bitmap = cached
            } else {
                fetchAvatarBase64Js(
                    url = validUrl,
                    onSuccess = { b64 ->
                        if (!disposed) {
                            val decoded = runCatching {
                                val bytes = decodeBase64Bytes(b64)
                                if (bytes.isNotEmpty()) {
                                    org.jetbrains.skia.Image.makeFromEncoded(bytes).toComposeImageBitmap()
                                } else {
                                    null
                                }
                            }.getOrNull()
                            if (decoded != null) {
                                avatarBitmapCache[validUrl] = decoded
                                bitmap = decoded
                            }
                        }
                    },
                    onError = {
                        if (!disposed) {
                            bitmap = null
                        }
                    },
                )
            }
        } else {
            bitmap = null
        }
        onDispose {
            disposed = true
        }
    }

    val initial = remember(name) {
        name.trim().take(1).uppercase().ifBlank { "?" }
    }

    Box(
        modifier = Modifier
            .size(size)
            .clip(CircleShape)
            .background(if (isHost) cs.primary else cs.secondaryContainer),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = initial,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.ExtraBold,
            color = if (isHost) cs.onPrimary else cs.onSecondaryContainer,
        )
        val loadedBitmap = bitmap
        if (loadedBitmap != null) {
            Image(
                bitmap = loadedBitmap,
                contentDescription = name,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxSize()
                    .clip(CircleShape),
            )
        }
    }
}

private fun fetchAvatarBase64Js(
    url: String,
    onSuccess: (String) -> Unit,
    onError: () -> Unit,
) {
    js("""
        (function() {
            try {
                fetch(url, { mode: 'cors', credentials: 'omit' })
                    .then(function(res) {
                        if (!res.ok) throw new Error('HTTP ' + res.status);
                        return res.arrayBuffer();
                    })
                    .then(function(buf) {
                        const bytes = new Uint8Array(buf);
                        if (!bytes || bytes.length === 0 || bytes.length > 5242880) {
                            onError();
                            return;
                        }
                        let binary = '';
                        const chunk = 8192;
                        for (let i = 0; i < bytes.length; i += chunk) {
                            binary += String.fromCharCode.apply(null, bytes.subarray(i, Math.min(i + chunk, bytes.length)));
                        }
                        onSuccess(btoa(binary));
                    })
                    .catch(function() {
                        onError();
                    });
            } catch (e) {
                onError();
            }
        })()
    """)
}

private fun decodeBase64Bytes(b64: String): ByteArray {
    val clean = b64.trim()
    if (clean.isEmpty()) return ByteArray(0)
    val table = IntArray(128) { -1 }
    val chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/"
    for (i in chars.indices) table[chars[i].code] = i
    val pad = when {
        clean.endsWith("==") -> 2
        clean.endsWith("=") -> 1
        else -> 0
    }
    val outLen = (clean.length * 3) / 4 - pad
    if (outLen <= 0) return ByteArray(0)
    val out = ByteArray(outLen)
    var outIdx = 0
    var acc = 0
    var bits = 0
    for (element in clean) {
        if (element == '=') break
        val code = element.code
        val v = if (code in 0..127) table[code] else -1
        if (v < 0) continue
        acc = (acc shl 6) or v
        bits += 6
        if (bits >= 8) {
            bits -= 8
            if (outIdx < outLen) {
                out[outIdx++] = ((acc shr bits) and 0xFF).toByte()
            }
        }
    }
    return if (outIdx == outLen) out else out.copyOf(outIdx)
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
