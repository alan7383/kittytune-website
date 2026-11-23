@file:OptIn(kotlin.js.ExperimentalWasmJsInterop::class)

package com.alananasss.kittytunewebsite.ui

import androidx.compose.runtime.Immutable

@Immutable
data class WebSharedTrack(
    val id: Long,
    val title: String,
    val artist: String,
    val artworkUrl: String?,
    val durationMs: Long,
    val permalinkUrl: String?,
    val source: String?,
)

@Immutable
data class WebMember(
    val id: String,
    val name: String,
    val isListening: Boolean,
    val lastSeenMs: Long,
    val avatarUrl: String? = null,
)

enum class WebRelayConnection {
    IDLE,
    CONNECTING,
    CONNECTED,
    OFFLINE,
}

@Immutable
data class TogetherWebSessionState(
    val code: String,
    val connection: WebRelayConnection = WebRelayConnection.IDLE,
    val initialSyncComplete: Boolean = false,
    val roomName: String = "",
    val hostId: String? = null,
    val hostName: String? = null,
    val hostAvatarUrl: String? = null,
    val hostSeenAtMs: Long = 0L,
    val hostLocked: Boolean = false,
    val automix: Boolean = false,
    val nowPlaying: WebSharedTrack? = null,
    val isPlaying: Boolean = false,
    val positionMs: Long = 0L,
    val stateReceivedAtMs: Long = 0L,
    val upcoming: List<WebSharedTrack> = emptyList(),
    val playlist: List<WebSharedTrack> = emptyList(),
    val members: Map<String, WebMember> = emptyMap(),
) {
    fun hasLiveHost(nowMs: Long): Boolean =
        hostId != null && (nowMs - hostSeenAtMs) < 25_000L

    fun activeListeners(nowMs: Long): List<WebMember> =
        members.values.filter { it.isListening && (nowMs - it.lastSeenMs) < 30_000L }

    fun resolvedHostName(): String? {
        if (!hostName.isNullOrBlank()) return hostName
        val fromMember = hostId?.let { members[it]?.name }?.takeIf { it.isNotBlank() }
        if (fromMember != null) return fromMember
        val fromRoom = roomName.substringBefore("·").trim().takeIf {
            it.isNotBlank() && roomName.contains("·")
        }
        return fromRoom
    }

    fun interpolatedPositionMs(nowMs: Long): Long {
        val dur = nowPlaying?.durationMs?.takeIf { it > 0L }
        val raw = if (isPlaying && stateReceivedAtMs > 0L) {
            positionMs + (nowMs - stateReceivedAtMs).coerceAtLeast(0L)
        } else {
            positionMs
        }
        return if (dur != null) raw.coerceIn(0L, dur) else raw.coerceAtLeast(0L)
    }
}

object TogetherWebLinks {
    const val CODE_LENGTH = 8
    private val shortInviteRegex = Regex(
        """https://kittytune\.pages\.dev/([A-Za-z0-9]{4}-?[A-Za-z0-9]{4})/?(?=$|[?#\s<>)\].,!])""",
        RegexOption.IGNORE_CASE,
    )

    fun normalizeCode(raw: String): String =
        raw.uppercase().filter { it in 'A'..'Z' || it in '0'..'9' }

    fun displayCode(code: String): String =
        normalizeCode(code).take(CODE_LENGTH).chunked(4).joinToString("-")

    fun extractCodeFromUrl(fullUrlOrQuery: String): String? {
        shortInviteRegex.find(fullUrlOrQuery)?.groupValues?.getOrNull(1)?.let { return normalizeCode(it) }

        val queryMatch = Regex(
            """[?&#](?:together|listen_along|room)=([A-Za-z0-9-]{8,12})""",
            RegexOption.IGNORE_CASE,
        ).find(fullUrlOrQuery)?.groupValues?.getOrNull(1)
        if (queryMatch != null) {
            val norm = normalizeCode(queryMatch)
            if (norm.length == CODE_LENGTH) return norm
        }

        val pathMatch = Regex(
            """/(?:together|listen)/([A-Za-z0-9-]{8,12})""",
            RegexOption.IGNORE_CASE,
        ).find(fullUrlOrQuery)?.groupValues?.getOrNull(1)
        if (pathMatch != null) {
            val norm = normalizeCode(pathMatch)
            if (norm.length == CODE_LENGTH) return norm
        }
        return null
    }

    fun extractUserInputCode(input: String): String? {
        val trimmed = input.trim()
        if (trimmed.isEmpty()) return null
        extractCodeFromUrl(trimmed)?.let { return it }
        val norm = normalizeCode(trimmed)
        return norm.takeIf { it.length == CODE_LENGTH && trimmed.length <= 14 }
    }

    fun appDeepLink(code: String): String = "kittytune://together/${displayCode(code)}"

    fun webShareUrl(baseOriginAndPath: String, code: String): String =
        // The invite page: link previews show its "you're invited" card, and it forwards to the session.
        "${baseOriginAndPath.trimEnd('/')}/together/?together=${displayCode(code)}"
}

internal fun sanitizeTogetherAvatarUrl(raw: String?): String? {
    val trimmed = raw?.trim()?.takeIf { it.isNotEmpty() } ?: return null
    if (!trimmed.startsWith("https://", ignoreCase = true)) return null
    if (trimmed.contains("default_avatar", ignoreCase = true)) return null
    return trimmed
}

/**
 * Applies a normalized tab/newline event emitted by the browser WebCrypto + MQTT-over-WSS bridge.
 */
internal fun reduceTogetherWebEvent(
    current: TogetherWebSessionState,
    rawPayload: String,
    nowMs: Long,
): TogetherWebSessionState {
    val lines = rawPayload.split('\n')
    if (lines.isEmpty()) return current
    val header = lines[0].split('\t')
    val kind = header.getOrNull(0)?.uppercase() ?: return current

    return when (kind) {
        "META" -> {
            when (header.getOrNull(1)) {
                "connecting" -> current.copy(
                    connection = if (current.connection == WebRelayConnection.CONNECTED) {
                        current.connection
                    } else {
                        WebRelayConnection.CONNECTING
                    },
                )
                "connected" -> current.copy(connection = WebRelayConnection.CONNECTED)
                "sync_settled" -> current.copy(initialSyncComplete = true)
                "offline" -> current.copy(connection = WebRelayConnection.OFFLINE)
                else -> current
            }
        }

        "HELLO" -> {
            val from = header.getOrNull(1).orEmpty()
            val name = header.getOrNull(2).orEmpty()
            val listening = header.getOrNull(3) == "1"
            val incomingAvatar = sanitizeTogetherAvatarUrl(header.getOrNull(4))
            if (from.isBlank()) return current
            val prev = current.members[from]
            val resolvedAvatar = incomingAvatar ?: prev?.avatarUrl
            val updatedMembers = current.members + (
                from to WebMember(
                    id = from,
                    name = name.ifBlank { prev?.name.orEmpty() },
                    isListening = listening,
                    lastSeenMs = nowMs,
                    avatarUrl = resolvedAvatar,
                )
            )
            val updatedHostName = if (current.hostId == from && name.isNotBlank()) name else current.hostName
            val updatedHostAvatar = if (current.hostId == from && resolvedAvatar != null) resolvedAvatar else current.hostAvatarUrl
            current.copy(
                connection = WebRelayConnection.CONNECTED,
                initialSyncComplete = current.initialSyncComplete || listening,
                members = updatedMembers,
                hostName = updatedHostName,
                hostAvatarUrl = updatedHostAvatar,
            )
        }

        "BYE" -> {
            val from = header.getOrNull(1).orEmpty()
            val existing = current.members[from]
            val updatedMembers = if (existing != null) {
                current.members + (from to existing.copy(isListening = false))
            } else {
                current.members
            }
            current.copy(
                members = updatedMembers,
                hostId = if (current.hostId == from) null else current.hostId,
            )
        }

        "CLAIM" -> {
            val from = header.getOrNull(1).orEmpty()
            if (from.isBlank()) return current
            current.copy(
                connection = WebRelayConnection.CONNECTED,
                initialSyncComplete = true,
                hostId = from,
                hostSeenAtMs = nowMs,
                hostName = current.members[from]?.name ?: current.hostName,
                hostAvatarUrl = current.members[from]?.avatarUrl
                    ?: if (current.hostId == from) current.hostAvatarUrl else null,
            )
        }

        "STATE" -> {
            val from = header.getOrNull(1).orEmpty()
            val posMs = header.getOrNull(2)?.toLongOrNull() ?: 0L
            val playing = header.getOrNull(3) == "1"
            val automix = header.getOrNull(4) == "1"
            val locked = header.getOrNull(5) == "1"
            val msgHostName = header.getOrNull(6)?.takeIf { it.isNotBlank() }
            val msgHostAvatar = sanitizeTogetherAvatarUrl(header.getOrNull(7))
            val trackLine = lines.getOrNull(1)?.takeIf { it.isNotBlank() && !it.startsWith("LISTENER\t") }
            val track = trackLine?.let(::parseTrackLine)

            var updatedMembers = current.members
            val prev = if (from.isNotBlank()) updatedMembers[from] else null
            val resolvedMemberAvatar = msgHostAvatar
                ?: prev?.avatarUrl
                ?: if (from.isNotBlank() && from == current.hostId) current.hostAvatarUrl else null
            val memberName = msgHostName ?: prev?.name
            if (from.isNotBlank() && (memberName != null || resolvedMemberAvatar != null)) {
                updatedMembers = updatedMembers + (
                    from to WebMember(
                        id = from,
                        name = memberName.orEmpty(),
                        isListening = prev?.isListening ?: true,
                        lastSeenMs = nowMs,
                        avatarUrl = resolvedMemberAvatar,
                    )
                )
            }

            for (idx in 1 until lines.size) {
                val line = lines[idx]
                if (!line.startsWith("LISTENER\t")) continue
                val lParts = line.split('\t')
                val lId = lParts.getOrNull(1).orEmpty()
                if (lId.isBlank()) continue
                val lName = lParts.getOrNull(2).orEmpty()
                val lAvatar = sanitizeTogetherAvatarUrl(lParts.getOrNull(3))
                val existing = updatedMembers[lId]
                val mergedAvatar = lAvatar
                    ?: existing?.avatarUrl
                    ?: if (lId == from) resolvedMemberAvatar else null
                val mergedName = lName.ifBlank {
                    existing?.name?.takeIf { it.isNotBlank() }
                        ?: if (lId == from) msgHostName.orEmpty() else ""
                }
                updatedMembers = updatedMembers + (
                    lId to WebMember(
                        id = lId,
                        name = mergedName,
                        isListening = true,
                        lastSeenMs = nowMs,
                        avatarUrl = mergedAvatar,
                    )
                )
            }

            val resolvedName = msgHostName
                ?: updatedMembers[from]?.name?.takeIf { it.isNotBlank() }
                ?: current.hostName
            val resolvedHostAvatar = msgHostAvatar
                ?: updatedMembers[from]?.avatarUrl
                ?: if (from.isBlank() || from == current.hostId) current.hostAvatarUrl else null

            current.copy(
                connection = WebRelayConnection.CONNECTED,
                initialSyncComplete = true,
                hostId = from.ifBlank { current.hostId },
                hostName = resolvedName,
                hostAvatarUrl = resolvedHostAvatar,
                hostSeenAtMs = nowMs,
                hostLocked = locked,
                automix = automix,
                nowPlaying = track,
                isPlaying = playing,
                positionMs = posMs,
                stateReceivedAtMs = nowMs,
                members = updatedMembers,
            )
        }

        "QUEUE" -> {
            val name = header.getOrNull(1).orEmpty()
            val upcomingCount = header.getOrNull(2)?.toIntOrNull() ?: 0
            val playlistCount = header.getOrNull(3)?.toIntOrNull() ?: 0
            val upcoming = lines.drop(1).take(upcomingCount).mapNotNull(::parseTrackLine)
            val playlist = lines.drop(1 + upcomingCount).take(playlistCount).mapNotNull(::parseTrackLine)
            current.copy(
                connection = WebRelayConnection.CONNECTED,
                roomName = name.ifBlank { current.roomName },
                upcoming = upcoming,
                playlist = if (playlist.isNotEmpty() || current.playlist.isEmpty()) playlist else current.playlist,
            )
        }

        else -> current
    }
}

private fun parseTrackLine(line: String): WebSharedTrack? {
    val parts = line.split('\t')
    if (parts.size < 3) return null
    val id = parts.getOrNull(0)?.toLongOrNull() ?: 0L
    val title = parts.getOrNull(1).orEmpty()
    val artist = parts.getOrNull(2).orEmpty()
    val artworkUrl = parts.getOrNull(3)?.takeIf { it.isNotBlank() }
    val durationMs = parts.getOrNull(4)?.toLongOrNull() ?: 0L
    val permalinkUrl = parts.getOrNull(5)?.takeIf { it.isNotBlank() }
    val source = parts.getOrNull(6)?.takeIf { it.isNotBlank() }
    return WebSharedTrack(
        id = id,
        title = title,
        artist = artist,
        artworkUrl = artworkUrl,
        durationMs = durationMs,
        permalinkUrl = permalinkUrl,
        source = source,
    )
}

internal fun nowEpochMsJs(): Long = nowEpochMsDoubleJs().toLong()

private fun nowEpochMsDoubleJs(): Double = js("Date.now()")

internal fun isFrenchBrowserJs(): Boolean = js("((window.navigator.language || '').toLowerCase().indexOf('fr') === 0)")

internal fun openExternalUrlJs(url: String) {
    js("window.open(url, '_blank', 'noopener,noreferrer')")
}

internal fun triggerAppDeepLinkJs(deepLink: String) {
    js("""
        try {
            window.location.href = deepLink;
        } catch (e) {
            console.warn('Deep link launch failed:', e);
        }
    """)
}

internal fun copyTextWebJs(text: String) {
    js("""
        try {
            const ta = document.createElement('textarea');
            ta.value = text;
            ta.setAttribute('readonly', '');
            ta.style.position = 'fixed';
            ta.style.opacity = '0';
            ta.style.pointerEvents = 'none';
            document.body.appendChild(ta);
            ta.select();
            ta.setSelectionRange(0, 99999);
            document.execCommand('copy');
            document.body.removeChild(ta);
        } catch (e) {}
        if (navigator.clipboard && navigator.clipboard.writeText) {
            navigator.clipboard.writeText(text).catch(function() {});
        }
    """)
}

/**
 * Attaches to a pre-warmed `window.__ktEarlyRelay` (started at `t = 0 ms` in `index.html` while
 * `.wasm` was downloading, synchronously replaying all buffered events in `0 ms` without tearing
 * down sockets) or starts a new shared `window.__ktStartRelay` session for manual code entry.
 */
internal fun startTogetherWebRelayJs(normalizedCode: String, onEvent: (String) -> Unit) {
    js("""
        (function() {
            if (window.__ktTogetherCleanup) {
                try { window.__ktTogetherCleanup(); } catch (e) {}
                window.__ktTogetherCleanup = null;
            }
            const clean = (normalizedCode || '').toUpperCase().replace(/[^A-Z0-9]/g, '');
            if (clean.length !== 8) {
                onEvent('META\toffline');
                return;
            }

            const forward = function(ev) {
                onEvent(String(ev));
            };

            const early = window.__ktEarlyRelay;
            if (early) {
                window.__ktEarlyRelay = null;
                if (!early.stopped && early.code === clean) {
                    window.__ktTogetherCleanup = function() {
                        try { early.stop(); } catch (e) {}
                    };
                    early.attach(forward);
                    return;
                }
                try { early.stop(); } catch (e) {}
            }

            if (typeof window.__ktStartRelay === 'function') {
                const relay = window.__ktStartRelay(clean);
                window.__ktTogetherCleanup = function() {
                    try { relay.stop(); } catch (e) {}
                };
                relay.attach(forward);
                return;
            }

            onEvent('META\toffline');
        })();
    """)
}

internal fun stopTogetherWebRelayJs() {
    js("""
        if (window.__ktTogetherCleanup) {
            try { window.__ktTogetherCleanup(); } catch (e) {}
            window.__ktTogetherCleanup = null;
        }
        if (window.__ktEarlyRelay) {
            try { window.__ktEarlyRelay.stop(); } catch (e) {}
            window.__ktEarlyRelay = null;
        }
    """)
}
