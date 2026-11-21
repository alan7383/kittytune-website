package com.alananasss.kittytunewebsite.ui.lyrics

/**
 * Original lyrics written for the site ("Neon Tides" by the fictional Mira Vale), timed word by word
 * like a TTML/QRC karaoke file. Held words (long durations) trigger the per-letter lift-and-swell,
 * short ones the soft fill, and the duet line is end-aligned like a second singer in the app.
 *
 * Copyrighted songs are deliberately not reproduced here: swap this list for licensed lyrics if needed.
 */
val NeonTidesLyrics: List<KLine> = listOf(
    scriptLine(600, "We|300 were|260 danc-|300 ing|320 on|260 the|220 ceil-|380 ing|520"),
    scriptLine(3500, "chas-|300 ing|280 ech-|320 oes|360 through|340 the|240 night|1700"),
    scriptLine(7300, "ev-|260 ery|260 heart-|360 beat|340 is|240 a|200 sig-|380 nal|520"),
    scriptLine(10200, "ev-|260 ery|260 shad-|320 ow|340 turns|360 to|260 light|1800"),
    scriptLine(14200, "(hold|420 on,|460 hold|420 on)|1200", alignEnd = true),
    scriptLine(17000, "Don't|300 let|260 the|220 mu-|320 sic|340 fade|420 a-|300 way|1500"),
    scriptLine(21000, "sing|360 it|240 loud-|360 er,|320 sing|360 it|240 high-|360 er|520"),
    scriptLine(24400, "'til|300 the|240 cit-|320 y|280 knows|380 our|300 name|1900"),
    scriptLine(28800, "Ooh,|1400 the|260 neon|560 tides|1600", alignEnd = true),
)

val NeonTidesLyricsFr: List<String> = listOf(
    "On dansait au plafond",
    "à chasser les échos dans la nuit",
    "chaque battement est un signal",
    "chaque ombre devient lumière",
    "(tiens bon, tiens bon)",
    "Ne laisse pas la musique s'éteindre",
    "chante plus fort, chante plus haut",
    "jusqu'à ce que la ville connaisse notre nom",
    "Ooh, les marées néon",
)

val NeonTidesLoopMs: Int = NeonTidesLyrics.last().end + 2200

fun neonTidesWithTranslation(tr: List<String>): List<KLine> =
    NeonTidesLyrics.mapIndexed { i, l -> l.copy(translation = tr.getOrNull(i)) }
