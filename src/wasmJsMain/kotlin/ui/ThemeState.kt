package com.alananasss.kittytunewebsite.ui

import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import com.materialkolor.PaletteStyle

data class ColorPreset(val name: String, val color: Color)

/** Vibrant blue first: it is the site default. */
val colorPresets = listOf(
    ColorPreset("Blue", Color(0xFF1A6DFF)),
    ColorPreset("Purple", Color(0xFF7B1FA2)),
    ColorPreset("Pink", Color(0xFFAD1457)),
    ColorPreset("Red", Color(0xFFC62828)),
    ColorPreset("Orange", Color(0xFFE65100)),
    ColorPreset("Gold", Color(0xFFF9A825)),
    ColorPreset("Green", Color(0xFF2E7D32)),
    ColorPreset("Teal", Color(0xFF00897B)),
    ColorPreset("Cyan", Color(0xFF00838F)),
    ColorPreset("Indigo", Color(0xFF283593)),
)

val paletteStyles = listOf(
    "Vibrant" to PaletteStyle.Vibrant,
    "Expressive" to PaletteStyle.Expressive,
    "Tonal Spot" to PaletteStyle.TonalSpot,
    "Rainbow" to PaletteStyle.Rainbow,
    "Fruit Salad" to PaletteStyle.FruitSalad,
    "Fidelity" to PaletteStyle.Fidelity,
    "Content" to PaletteStyle.Content,
    "Monochrome" to PaletteStyle.Monochrome,
    "Neutral" to PaletteStyle.Neutral,
)

/** Lets any demo drive the site theme (the dynamic-colour card re-themes the whole page). */
class ThemeController(
    val selectedColor: ColorPreset,
    val selectColor: (ColorPreset) -> Unit,
    val selectedStyle: Pair<String, PaletteStyle>,
    val selectStyle: (Pair<String, PaletteStyle>) -> Unit,
    val isDark: Boolean,
    val setDark: (Boolean) -> Unit,
)

val LocalThemeController = staticCompositionLocalOf<ThemeController?> { null }
