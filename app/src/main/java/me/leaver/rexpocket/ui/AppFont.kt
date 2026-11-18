package me.leaver.rexpocket.ui

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

/**
 * Google Sans Flex, the app's typeface, fetched once from Google Fonts and kept in the app's files.
 * Until it is on the phone - and on a phone that cannot reach Google - the system font stands in;
 * nothing waits for it.
 */
internal object AppFont {
    private val weights = listOf(400, 500, 600, 700)
    private const val CSS = "https://fonts.googleapis.com/css2?family=Google+Sans+Flex:wght@400;500;600;700"

    private fun files(context: Context) = weights.map { it to File(context.filesDir, "app-font/google-sans-flex-$it.ttf") }

    private fun family(files: List<Pair<Int, File>>) = FontFamily(
        files.map { (weight, file) ->
            Font(file, FontWeight(weight), variationSettings = FontVariation.Settings(FontVariation.weight(weight)))
        },
    )

    /** The family when it is already on the phone, so the first frame is set in it. */
    fun cached(context: Context): FontFamily? =
        files(context).takeIf { list -> list.all { it.second.length() > 0 } }?.let(::family)

    suspend fun download(context: Context): FontFamily? = withContext(Dispatchers.IO) {
        runCatching {
            val css = fetch(CSS).decodeToString()
            val urls = Regex("""font-weight:\s*(\d+);[\s\S]*?url\((https://[^)]+\.ttf)\)""").findAll(css)
                .associate { it.groupValues[1].toInt() to it.groupValues[2] }
            val targets = files(context)
            targets.forEach { (weight, file) ->
                if (file.length() > 0) return@forEach
                val bytes = fetch(urls[weight] ?: error("no $weight"))
                file.parentFile?.mkdirs()
                // Written whole, then moved, so a half-downloaded face is never read as the font.
                val part = File(file.path + ".part").apply { writeBytes(bytes) }
                check(part.renameTo(file))
            }
            family(targets)
        }.getOrNull()
    }

    private fun fetch(url: String): ByteArray {
        val connection = URL(url).openConnection() as HttpURLConnection
        connection.connectTimeout = 15_000
        connection.readTimeout = 30_000
        // Google Fonts serves TrueType to an Android user agent.
        connection.setRequestProperty("User-Agent", "Mozilla/5.0 (Linux; Android 14)")
        return try {
            check(connection.responseCode == 200) { "HTTP ${connection.responseCode}" }
            connection.inputStream.use { it.readBytes() }
        } finally {
            connection.disconnect()
        }
    }
}

/** Google Sans Flex once it is on the phone; null meanwhile, or when [wanted] is false. */
@Composable
internal fun rememberAppFont(wanted: Boolean): FontFamily? {
    val context = LocalContext.current
    // Previews and screenshot tests render without a network or a real files directory.
    if (LocalInspectionMode.current) return null
    var family by remember { mutableStateOf(AppFont.cached(context)) }
    LaunchedEffect(wanted) {
        if (wanted && family == null) family = AppFont.download(context)
    }
    return if (wanted) family else null
}
