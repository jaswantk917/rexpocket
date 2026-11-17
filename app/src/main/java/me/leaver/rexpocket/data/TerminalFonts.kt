package me.leaver.rexpocket.data

import android.graphics.Typeface
import android.graphics.fonts.Font
import android.graphics.fonts.FontFamily
import android.graphics.fonts.FontStyle
import android.os.Build
import java.io.File

internal fun downloadedTypeface(files: File, font: TerminalFamily): Typeface {
    if (Build.VERSION.SDK_INT < 29) return Typeface.createFromFile(File(files, font.regular))
    val family = FontFamily.Builder(Font.Builder(File(files, font.regular)).build())
    for ((name, weight, slant) in listOf(
        Triple(font.bold, 700, FontStyle.FONT_SLANT_UPRIGHT),
        Triple(font.italic, 400, FontStyle.FONT_SLANT_ITALIC),
        Triple(font.boldItalic, 700, FontStyle.FONT_SLANT_ITALIC),
    )) family.addFont(Font.Builder(File(files, name)).setWeight(weight).setSlant(slant).build())
    return Typeface.CustomFallbackBuilder(family.build()).setSystemFallback("sans-serif").build()
}

internal data class TerminalFontState(
    /** The family in use, by [TerminalFamily.id]; null is the system's monospace. */
    val selected: String? = null,
    /** The families saved on this phone, with their faces for the picker's preview. */
    val available: Map<String, Typeface> = emptyMap(),
    val initializing: Boolean = true,
    /** The family being downloaded, if any. */
    val downloading: String? = null,
    val progress: Float = 0f,
    /** The family whose last download failed, so the picker can say which to retry. */
    val failed: String? = null,
)
