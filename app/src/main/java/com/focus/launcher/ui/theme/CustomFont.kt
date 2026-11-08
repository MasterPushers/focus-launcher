package com.focus.launcher.ui.theme

import android.content.Context
import android.graphics.Paint
import android.graphics.Typeface
import android.net.Uri
import java.io.File

/**
 * The one user-imported TTF/OTF font ([com.focus.launcher.data.FontChoice.CUSTOM]). It is copied
 * into the app's own files, so it stays usable after the picked file is moved or deleted. A new
 * import replaces it. It is not part of the settings backup (that file holds only JSON).
 */
object CustomFont {
    @Volatile private var cached: Typeface? = null

    private fun file(context: Context) = File(context.filesDir, "fonts/custom")

    fun exists(context: Context) = file(context).isFile

    /** Copies [uri] in, if it is a font Android can load. False leaves the previous font untouched. */
    fun import(context: Context, uri: Uri): Boolean {
        val target = file(context)
        target.parentFile?.mkdirs()
        val tmp = File(target.parentFile, "custom.tmp")
        return try {
            val copied = context.contentResolver.openInputStream(uri)?.use { input ->
                tmp.outputStream().use { input.copyTo(it) }
            } != null
            val loaded = if (copied) Typeface.Builder(tmp).build() else null
            if (loaded == null || !tmp.renameTo(target)) {
                tmp.delete()
                false
            } else {
                cached = loaded
                true
            }
        } catch (_: Exception) {
            tmp.delete()
            false
        }
    }

    /** The imported font, or null when there is none (or it no longer loads). */
    fun typeface(context: Context): Typeface? = cached ?: file(context).takeIf { it.isFile }
        ?.let { Typeface.Builder(it).build() }
        ?.also { cached = it }

    /** Width of "00:00" in em, which sizes the split clock's time. */
    fun emsWide(typeface: Typeface): Float =
        Paint().apply { this.typeface = typeface; textSize = 100f }.measureText("00:00") / 100f
}
