package me.leaver.rexpocket.data

import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest

internal data class FontAsset(val name: String, val sha256: String)

/** Installs a complete verified family; incomplete downloads never become the active font. */
internal class FontFiles(private val root: File, private val family: TerminalFamily) {
    private val assets = family.assets
    val directory = File(root, family.directory)

    fun available(): Boolean = assets.all { matches(File(directory, it.name), it.sha256) }

    fun install(
        progress: (Float) -> Unit = {},
        fetch: (FontAsset, File) -> Unit = ::download,
    ): File {
        if (available()) return directory
        check(root.isDirectory || root.mkdirs())
        val staging = File(root, "${family.directory}.partial")
        staging.deleteRecursively()
        check(staging.mkdirs())
        try {
            assets.forEachIndexed { index, asset ->
                val target = File(staging, asset.name)
                fetch(asset, target)
                check(matches(target, asset.sha256)) { "Font checksum mismatch" }
                progress((index + 1f) / assets.size)
            }
            directory.deleteRecursively()
            check(staging.renameTo(directory)) { "Could not save fonts" }
            return directory
        } finally {
            staging.deleteRecursively()
        }
    }

    private fun matches(file: File, expected: String): Boolean = runCatching {
        if (!file.isFile || file.length() !in 1..family.maxFileBytes) return false
        val digest = MessageDigest.getInstance("SHA-256")
        file.inputStream().use { input ->
            val buffer = ByteArray(8192)
            while (true) {
                val count = input.read(buffer)
                if (count < 0) break
                digest.update(buffer, 0, count)
            }
        }
        digest.digest().joinToString("") { "%02x".format(it) } == expected
    }.getOrDefault(false)

    private fun download(asset: FontAsset, target: File) {
        // Some families name their files with spaces ("MesloLGS NF Regular.ttf").
        val connection = URL("${family.source}/${asset.name.replace(" ", "%20")}").openConnection() as HttpURLConnection
        connection.connectTimeout = 15_000
        connection.readTimeout = 15_000
        try {
            check(connection.responseCode == 200) { "Font download failed" }
            connection.inputStream.use { input ->
                target.outputStream().use { output ->
                    val buffer = ByteArray(8192)
                    var total = 0L
                    while (true) {
                        val count = input.read(buffer)
                        if (count < 0) break
                        total += count
                        check(total <= family.maxFileBytes) { "Font download too large" }
                        output.write(buffer, 0, count)
                    }
                }
            }
        } finally {
            connection.disconnect()
        }
    }
}
