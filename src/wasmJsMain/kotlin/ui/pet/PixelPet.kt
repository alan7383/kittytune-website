package com.alananasss.kittytunewebsite.ui.pet

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.alananasss.kittytunewebsite.ui.kit.rememberClock

class PetAnimation(
    val width: Int,
    val height: Int,
    val durationsMs: IntArray,
    val frames: List<String>,
) {
    val totalMs: Int = durationsMs.sum()

    /** Frames decoded to palette indices (-1 = transparent). */
    internal val decoded: List<IntArray> by lazy {
        frames.map { f -> IntArray(f.length) { i -> val c = f[i]; if (c == '.') -1 else PetPaletteKeys.indexOf(c) } }
    }

    fun frameAt(ms: Int): Int {
        var acc = ms % totalMs
        for (i in durationsMs.indices) {
            if (acc < durationsMs[i]) return i
            acc -= durationsMs[i]
        }
        return 0
    }
}

enum class PetMood(val anim: () -> PetAnimation) {
    Dj({ Pet_dj }), Hello({ Pet_hello }), Love({ Pet_love }), Headbang({ Pet_headbang }),
    Disco({ Pet_disco }), Loaf({ Pet_loaf }), Purr({ Pet_purr }), Guitar({ Pet_guitar }),
}

/**
 * Renders one of the Kitty Pet sprite animations pixel-for-pixel with Compose. When [tint] is set,
 * every opaque pixel is drawn in the palette colour blended toward the tint, so the cat follows the theme.
 */
@Composable
fun PixelPet(
    mood: PetMood,
    modifier: Modifier = Modifier,
    tint: Color? = null,
    tintAmount: Float = 0f,
    speed: Float = 1f,
) {
    val anim = remember(mood) { mood.anim() }
    val palette = remember(tint, tintAmount) {
        PetPalette.map { argb ->
            val base = Color(argb.toInt())
            if (tint == null || tintAmount <= 0f) base
            else Color(
                red = base.red + (tint.red - base.red) * tintAmount,
                green = base.green + (tint.green - base.green) * tintAmount,
                blue = base.blue + (tint.blue - base.blue) * tintAmount,
            )
        }
    }
    // Every frame of the sprite is rasterised once into a tiny bitmap and then scaled up with
    // nearest-neighbour sampling: one drawImage per frame instead of thousands of drawRect calls.
    val bitmaps = remember(anim, palette) {
        val paint = androidx.compose.ui.graphics.Paint()
        anim.decoded.map { frame ->
            val bmp = androidx.compose.ui.graphics.ImageBitmap(anim.width, anim.height)
            val c = androidx.compose.ui.graphics.Canvas(bmp)
            for (y in 0 until anim.height) {
                val row = y * anim.width
                for (x in 0 until anim.width) {
                    val idx = frame[row + x]
                    if (idx >= 0) {
                        paint.color = palette[idx]
                        c.drawRect(x.toFloat(), y.toFloat(), x + 1f, y + 1f, paint)
                    }
                }
            }
            bmp
        }
    }
    val clock = rememberClock(speed)
    Canvas(modifier.aspectRatio(anim.width.toFloat() / anim.height)) {
        val bmp = bitmaps[anim.frameAt((clock.value * 1000f).toInt())]
        drawImage(
            bmp,
            dstSize = androidx.compose.ui.unit.IntSize(size.width.toInt(), size.height.toInt()),
            filterQuality = androidx.compose.ui.graphics.FilterQuality.None,
        )
    }
}
