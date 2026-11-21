package com.alananasss.kittytunewebsite.ui.lyrics

import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember

/**
 * The lyrics card plays a real song, so it fetches its synced lyrics at runtime from LRCLIB — one of
 * the 14 providers the app itself uses — instead of shipping the words inside the site. Until the
 * request answers (or if it fails) the original "Neon Tides" demo keeps the card alive.
 */
data class LiveSong(
    val title: String,
    val artist: String,
    val durationMs: Int,
    val lrclibId: Long,
)

val Chatiment = LiveSong(title = "Châtiment", artist = "Sokuu", durationMs = 138_000, lrclibId = 9818783)

private val lyricsCache = HashMap<Long, List<KLine>>()
private val lyricsState = HashMap<Long, androidx.compose.runtime.MutableState<List<KLine>?>>()

/** Synced lines for [song], or null while they load / if the provider is unreachable. */
@Composable
fun rememberLiveLyrics(song: LiveSong): State<List<KLine>?> = remember(song.lrclibId) {
    lyricsState.getOrPut(song.lrclibId) {
        val state = mutableStateOf(lyricsCache[song.lrclibId])
        if (state.value == null) {
            fetchSyncedLyricsJs(song.lrclibId.toString()) { lrc ->
                val parsed = runCatching { parseLrc(lrc, song.durationMs) }.getOrNull()
                if (!parsed.isNullOrEmpty()) {
                    lyricsCache[song.lrclibId] = parsed
                    state.value = parsed
                }
            }
        }
        state
    }
}

private val lrcTag = Regex("""\[(\d+):(\d+)(?:[.:](\d+))?]""")

/**
 * Line-synced LRC → karaoke lines. Each word gets a share of the line proportional to its length, over
 * the time the line is actually sung, so the engine's soft fill sweeps across it like in the app.
 */
fun parseLrc(lrc: String, durationMs: Int): List<KLine> {
    val raw = lrc.lines().mapNotNull { l ->
        val m = lrcTag.find(l) ?: return@mapNotNull null
        val (mm, ss, frac) = m.destructured
        val f = when (frac.length) { 0 -> 0; 1 -> frac.toInt() * 100; 2 -> frac.toInt() * 10; else -> frac.take(3).toInt() }
        val t = mm.toInt() * 60_000 + ss.toInt() * 1000 + f
        t to l.substring(m.range.last + 1).trim()
    }.sortedBy { it.first }
    val out = mutableListOf<KLine>()
    raw.forEachIndexed { i, (start, text) ->
        if (text.isBlank()) return@forEachIndexed
        val next = raw.getOrNull(i + 1)?.first ?: durationMs
        val span = ((next - start) * 0.9f).toInt().coerceIn(600, 6000)
        val words = text.split(Regex("\\s+")).filter { it.isNotEmpty() }
        val totalChars = words.sumOf { it.length + 1 }.coerceAtLeast(1)
        var t = start
        val syl = words.mapIndexed { wi, w ->
            val d = (span * (w.length + 1) / totalChars).coerceAtLeast(90)
            KSyllable(if (wi == words.lastIndex) w else "$w ", t, t + d).also { t += d }
        }
        out += KLine(syl)
    }
    return out
}

private fun fetchSyncedLyricsJs(id: String, onResult: (String) -> Unit) {
    js("""
        fetch('https://lrclib.net/api/get/' + id, { mode: 'cors', credentials: 'omit' })
            .then(function(r) { if (!r.ok) throw new Error('HTTP ' + r.status); return r.json(); })
            .then(function(d) { if (d && d.syncedLyrics) onResult(d.syncedLyrics); })
            .catch(function() {});
    """)
}
