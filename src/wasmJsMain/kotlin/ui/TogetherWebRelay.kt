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
    val roomName: String = "",
    val hostId: String? = null,
    val hostName: String? = null,
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

    fun normalizeCode(raw: String): String =
        raw.uppercase().filter { it in 'A'..'Z' || it in '0'..'9' }

    fun displayCode(code: String): String =
        normalizeCode(code).take(CODE_LENGTH).chunked(4).joinToString("-")

    fun extractCodeFromUrl(fullUrlOrQuery: String): String? {
        val queryMatch = Regex(
            """[?&](?:together|listen_along|room)=([A-Za-z0-9-]{8,12})""",
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

    fun appDeepLink(code: String, autoJoin: Boolean = true): String {
        val suffix = if (autoJoin) "?join=1" else ""
        return "kittytune://together/${displayCode(code)}$suffix"
    }

    fun webShareUrl(baseOriginAndPath: String, code: String): String =
        "${baseOriginAndPath.trimEnd('/')}/?together=${displayCode(code)}"
}

/**
 * Applies a normalized pipe/field event emitted by the browser WebCrypto + MQTT-over-WSS bridge.
 */
internal fun reduceTogetherWebEvent(
    current: TogetherWebSessionState,
    rawPayload: String,
    nowMs: Long,
): TogetherWebSessionState {
    val lines = rawPayload.split('\n')
    if (lines.isEmpty()) return current
    val header = lines[0].split('\t')
    val kind = header.getOrNull(0) ?: return current

    return when (kind) {
        "META" -> {
            val status = when (header.getOrNull(1)) {
                "connecting" -> if (current.connection == WebRelayConnection.CONNECTED) current.connection else WebRelayConnection.CONNECTING
                "connected" -> WebRelayConnection.CONNECTED
                "offline" -> WebRelayConnection.OFFLINE
                else -> current.connection
            }
            current.copy(connection = status)
        }

        "HELLO" -> {
            val from = header.getOrNull(1).orEmpty()
            val name = header.getOrNull(2).orEmpty()
            val listening = header.getOrNull(3) == "1"
            if (from.isBlank()) return current
            val updatedMembers = current.members + (from to WebMember(from, name, listening, nowMs))
            val updatedHostName = if (current.hostId == from && name.isNotBlank()) name else current.hostName
            current.copy(
                connection = WebRelayConnection.CONNECTED,
                members = updatedMembers,
                hostName = updatedHostName,
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
                hostId = from,
                hostSeenAtMs = nowMs,
                hostName = current.members[from]?.name ?: current.hostName,
            )
        }

        "STATE" -> {
            val from = header.getOrNull(1).orEmpty()
            val posMs = header.getOrNull(2)?.toLongOrNull() ?: 0L
            val playing = header.getOrNull(3) == "1"
            val automix = header.getOrNull(4) == "1"
            val locked = header.getOrNull(5) == "1"
            val msgHostName = header.getOrNull(6)?.takeIf { it.isNotBlank() }
            val track = lines.getOrNull(1)?.takeIf { it.isNotBlank() }?.let(::parseTrackLine)

            val updatedMembers = if (from.isNotBlank() && msgHostName != null) {
                val prev = current.members[from]
                current.members + (from to WebMember(from, msgHostName, prev?.isListening ?: true, nowMs))
            } else {
                current.members
            }
            val resolvedName = msgHostName ?: updatedMembers[from]?.name ?: current.hostName

            current.copy(
                connection = WebRelayConnection.CONNECTED,
                hostId = from.ifBlank { current.hostId },
                hostName = resolvedName,
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
 * Connects passively over WebSocket MQTT 3.1.1 to the same encrypted public brokers used by
 * KittyTune Android & Desktop (`broker.emqx.io` and `broker.hivemq.com`), derives the identical
 * SHA-256 topic and AES-256-GCM key from [normalizedCode], sends a sealed `SyncRequest`, and
 * emits tab-separated lines to [onEvent] for every decrypted room message.
 */
internal fun startTogetherWebRelayJs(normalizedCode: String, onEvent: (String) -> Unit) {
    js("""
        (function() {
            if (window.__ktTogetherCleanup) {
                try { window.__ktTogetherCleanup(); } catch (e) {}
                window.__ktTogetherCleanup = null;
            }
            const clean = (normalizedCode || '').toUpperCase().replace(/[^A-Z0-9]/g, '');
            if (clean.length !== 8 || !window.crypto || !window.crypto.subtle) {
                onEvent('META\toffline');
                return;
            }

            let stopped = false;
            const sockets = [];
            const timers = [];
            const webMemberId = 'kt-web-' + Math.random().toString(36).slice(2, 10);
            const enc = new TextEncoder();
            const dec = new TextDecoder();

            function cleanField(v) {
                if (v === null || v === undefined) return '';
                return String(v).replace(/[\t\r\n]/g, ' ').trim();
            }

            function encodeTrack(t) {
                if (!t) return '';
                const artist = cleanField(t.artist) || cleanField(t.username);
                let art = cleanField(t.artworkUrl);
                if (art.includes('-large.')) art = art.replace('-large.', '-t500x500.');
                return [
                    t.id || 0,
                    cleanField(t.title),
                    artist,
                    art,
                    t.durationMs || 0,
                    cleanField(t.permalinkUrl),
                    cleanField(t.source)
                ].join('\t');
            }

            function encodeVarInt(len) {
                const out = [];
                let x = len;
                do {
                    let b = x % 128;
                    x = Math.floor(x / 128);
                    if (x > 0) b = b | 128;
                    out.push(b);
                } while (x > 0);
                return out;
            }

            function buildConnectPacket(clientId) {
                const cid = enc.encode(clientId);
                const body = [
                    0x00, 0x04, 0x4d, 0x51, 0x54, 0x54,
                    0x04,
                    0x02,
                    0x00, 0x1e,
                    (cid.length >> 8) & 0xff, cid.length & 0xff
                ];
                const rem = encodeVarInt(body.length + cid.length);
                const pkt = new Uint8Array(1 + rem.length + body.length + cid.length);
                pkt[0] = 0x10;
                pkt.set(rem, 1);
                pkt.set(body, 1 + rem.length);
                pkt.set(cid, 1 + rem.length + body.length);
                return pkt;
            }

            function buildSubscribePacket(topicStr) {
                const t = enc.encode(topicStr);
                const remLen = 2 + 2 + t.length + 1;
                const rem = encodeVarInt(remLen);
                const pkt = new Uint8Array(1 + rem.length + remLen);
                let o = 0;
                pkt[o++] = 0x82;
                pkt.set(rem, o); o += rem.length;
                pkt[o++] = 0x00; pkt[o++] = 0x01;
                pkt[o++] = (t.length >> 8) & 0xff;
                pkt[o++] = t.length & 0xff;
                pkt.set(t, o); o += t.length;
                pkt[o] = 0x00;
                return pkt;
            }

            function buildPublishPacket(topicStr, payloadBytes) {
                const t = enc.encode(topicStr);
                const remLen = 2 + t.length + payloadBytes.length;
                const rem = encodeVarInt(remLen);
                const pkt = new Uint8Array(1 + rem.length + remLen);
                let o = 0;
                pkt[o++] = 0x30;
                pkt.set(rem, o); o += rem.length;
                pkt[o++] = (t.length >> 8) & 0xff;
                pkt[o++] = t.length & 0xff;
                pkt.set(t, o); o += t.length;
                pkt.set(payloadBytes, o);
                return pkt;
            }

            onEvent('META\tconnecting');

            (async function init() {
                try {
                    const topicDigest = new Uint8Array(await crypto.subtle.digest('SHA-256', enc.encode('topic:' + clean)));
                    let topicHex = '';
                    for (let i = 0; i < topicDigest.length; i++) {
                        topicHex += topicDigest[i].toString(16).padStart(2, '0');
                    }
                    const topic = 'kittytune/together/' + topicHex.slice(0, 32);

                    const keyDigest = await crypto.subtle.digest('SHA-256', enc.encode('key:kittytune-together:' + clean));
                    const aesKey = await crypto.subtle.importKey('raw', keyDigest, { name: 'AES-GCM' }, false, ['encrypt', 'decrypt']);

                    if (stopped) return;

                    async function sealMessage(obj) {
                        const iv = crypto.getRandomValues(new Uint8Array(12));
                        const plain = enc.encode(JSON.stringify(obj));
                        const ct = new Uint8Array(await crypto.subtle.encrypt({ name: 'AES-GCM', iv: iv, tagLength: 128 }, aesKey, plain));
                        const combined = new Uint8Array(12 + ct.length);
                        combined.set(iv, 0);
                        combined.set(ct, 12);
                        return combined;
                    }

                    async function openMessage(bytes) {
                        if (!bytes || bytes.length <= 12) return null;
                        try {
                            const iv = bytes.slice(0, 12);
                            const ct = bytes.slice(12);
                            const plain = await crypto.subtle.decrypt({ name: 'AES-GCM', iv: iv, tagLength: 128 }, aesKey, ct);
                            return JSON.parse(dec.decode(plain));
                        } catch (e) {
                            return null;
                        }
                    }

                    let activeCount = 0;
                    let receivedState = false;

                    function handleDecoded(msg) {
                        if (!msg || !msg.type || msg.from === webMemberId) return;
                        if (msg.type === 'Hello') {
                            onEvent(['HELLO', cleanField(msg.from), cleanField(msg.name), msg.isListening ? '1' : '0'].join('\t'));
                        } else if (msg.type === 'Bye') {
                            onEvent(['BYE', cleanField(msg.from)].join('\t'));
                        } else if (msg.type === 'ClaimHost') {
                            onEvent(['CLAIM', cleanField(msg.from)].join('\t'));
                        } else if (msg.type === 'State') {
                            receivedState = true;
                            const head = [
                                'STATE',
                                cleanField(msg.from),
                                Math.max(0, Math.floor(msg.positionMs || 0)),
                                msg.isPlaying ? '1' : '0',
                                msg.automix ? '1' : '0',
                                msg.hostLocked ? '1' : '0',
                                cleanField(msg.hostName)
                            ].join('\t');
                            const tLine = msg.track ? encodeTrack(msg.track) : '';
                            onEvent(tLine ? (head + '\n' + tLine) : head);
                        } else if (msg.type === 'Queue') {
                            const up = Array.isArray(msg.upcoming) ? msg.upcoming.slice(0, 20) : [];
                            const pl = Array.isArray(msg.playlist) ? msg.playlist.slice(0, 30) : [];
                            const lines = [
                                ['QUEUE', cleanField(msg.name), up.length, pl.length].join('\t')
                            ];
                            for (let i = 0; i < up.length; i++) lines.push(encodeTrack(up[i]));
                            for (let i = 0; i < pl.length; i++) lines.push(encodeTrack(pl[i]));
                            onEvent(lines.join('\n'));
                        }
                    }

                    const brokers = [
                        'wss://broker.emqx.io:8084/mqtt',
                        'wss://broker.hivemq.com:8884/mqtt'
                    ];

                    brokers.forEach(function(brokerUrl, idx) {
                        let ws = null;
                        let buf = new Uint8Array(0);
                        let mqttReady = false;

                        async function sendSyncReq() {
                            if (stopped || !ws || ws.readyState !== 1 || !mqttReady) return;
                            try {
                                const sealed = await sealMessage({
                                    from: webMemberId,
                                    sentAt: Date.now(),
                                    type: 'SyncRequest'
                                });
                                if (!stopped && ws && ws.readyState === 1) {
                                    ws.send(buildPublishPacket(topic, sealed));
                                }
                            } catch (e) {}
                        }

                        function openSocket() {
                            if (stopped) return;
                            try {
                                ws = new WebSocket(brokerUrl, 'mqtt');
                                ws.binaryType = 'arraybuffer';
                                sockets.push(ws);
                            } catch (e) {
                                return;
                            }

                            ws.onopen = function() {
                                if (stopped) { try { ws.close(); } catch (e) {} return; }
                                ws.send(buildConnectPacket(webMemberId + '-' + idx + '-' + (Date.now() % 9999)));
                            };

                            ws.onmessage = function(ev) {
                                if (stopped || !(ev.data instanceof ArrayBuffer)) return;
                                const incoming = new Uint8Array(ev.data);
                                const merged = new Uint8Array(buf.length + incoming.length);
                                merged.set(buf, 0);
                                merged.set(incoming, buf.length);
                                buf = merged;

                                while (buf.length >= 2) {
                                    let mult = 1;
                                    let remLen = 0;
                                    let pos = 1;
                                    let encodedByte = 0;
                                    let malformed = false;
                                    do {
                                        if (pos >= buf.length) return;
                                        encodedByte = buf[pos++];
                                        remLen += (encodedByte & 127) * mult;
                                        mult *= 128;
                                        if (mult > 128 * 128 * 128 * 128) { malformed = true; break; }
                                    } while ((encodedByte & 128) !== 0);

                                    if (malformed) { buf = new Uint8Array(0); return; }
                                    const totalLen = pos + remLen;
                                    if (buf.length < totalLen) return;

                                    const pktHeader = buf[0];
                                    const pktBody = buf.slice(pos, totalLen);
                                    buf = buf.slice(totalLen);

                                    const pktType = pktHeader & 0xf0;
                                    if (pktType === 0x20) {
                                        mqttReady = true;
                                        activeCount++;
                                        onEvent('META\tconnected');
                                        ws.send(buildSubscribePacket(topic));
                                        sendSyncReq();
                                        timers.push(setTimeout(function() {
                                            if (!receivedState) sendSyncReq();
                                        }, 2200));
                                    } else if (pktType === 0x30 && pktBody.length >= 2) {
                                        const qos = (pktHeader >> 1) & 0x03;
                                        const tLen = (pktBody[0] << 8) | pktBody[1];
                                        const offset = 2 + tLen + (qos > 0 ? 2 : 0);
                                        if (offset < pktBody.length) {
                                            const cipherBytes = pktBody.slice(offset);
                                            openMessage(cipherBytes).then(function(decoded) {
                                                if (!stopped && decoded) handleDecoded(decoded);
                                            });
                                        }
                                    }
                                }
                            };

                            ws.onclose = function() {
                                if (mqttReady) {
                                    mqttReady = false;
                                    activeCount = Math.max(0, activeCount - 1);
                                    if (!stopped && activeCount === 0) {
                                        onEvent('META\toffline');
                                    }
                                }
                                if (!stopped) {
                                    timers.push(setTimeout(openSocket, 5000));
                                }
                            };
                        }

                        openSocket();

                        timers.push(setInterval(function() {
                            if (!stopped && ws && ws.readyState === 1 && mqttReady) {
                                try { ws.send(new Uint8Array([0xc0, 0x00])); } catch (e) {}
                                if (!receivedState) sendSyncReq();
                            }
                        }, 18000));
                    });
                } catch (e) {
                    if (!stopped) onEvent('META\toffline');
                }
            })();

            window.__ktTogetherCleanup = function() {
                stopped = true;
                for (let i = 0; i < timers.length; i++) {
                    clearTimeout(timers[i]);
                    clearInterval(timers[i]);
                }
                for (let i = 0; i < sockets.length; i++) {
                    try { sockets[i].close(); } catch (e) {}
                }
            };
        })();
    """)
}

internal fun stopTogetherWebRelayJs() {
    js("""
        if (window.__ktTogetherCleanup) {
            try { window.__ktTogetherCleanup(); } catch (e) {}
            window.__ktTogetherCleanup = null;
        }
    """)
}
