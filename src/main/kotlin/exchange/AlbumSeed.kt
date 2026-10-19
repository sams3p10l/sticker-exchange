package exchange

import exchange.persistence.TrialAlbums
import exchange.persistence.TrialSticker
import org.slf4j.LoggerFactory
import kotlin.system.exitProcess

private val log = LoggerFactory.getLogger("exchange.album")
private const val SEED_PATH = "seed/trial-album.txt"
private val codePattern = Regex("^[A-Za-z0-9]{1,16}$")

fun seedTrialAlbum(albums: TrialAlbums) {
    val text = readSeed()
    val parsed = parseSeed(text)
    if (albums.exists(parsed.slug)) {
        log.info("Trial album already seeded ({})", parsed.slug)
        return
    }
    albums.insert(parsed.publisher, parsed.title, parsed.slug, parsed.stickers)
    log.info("Seeded trial album {} with {} stickers", parsed.slug, parsed.stickers.size)
}

private data class ParsedSeed(
    val publisher: String,
    val title: String,
    val slug: String,
    val stickers: List<TrialSticker>,
)

private fun readSeed(): String {
    val stream = Thread.currentThread().contextClassLoader.getResourceAsStream(SEED_PATH)
        ?: AlbumSeed::class.java.classLoader.getResourceAsStream(SEED_PATH)
    if (stream == null) {
        failSeed("missing $SEED_PATH")
    }
    return stream.bufferedReader().readText()
}

private object AlbumSeed

private fun parseSeed(text: String): ParsedSeed {
    val normalized = text.replace("\r\n", "\n")
    val parts = normalized.split("\n---\n", limit = 2)
    if (parts.size != 2) {
        failSeed("missing sticker list")
    }
    val header = mutableMapOf<String, String>()
    parts[0].lineSequence().forEach { line ->
        val trimmed = line.trim()
        if (trimmed.isEmpty() || trimmed.startsWith("#")) return@forEach
        val pieces = trimmed.split(":", limit = 2)
        if (pieces.size != 2 || pieces[1].isBlank()) {
            failSeed("bad header line")
        }
        header[pieces[0].trim()] = pieces[1].trim()
    }
    val publisher = header["publisher"] ?: failSeed("publisher is missing")
    val title = header["title"] ?: failSeed("title is missing")
    val slug = header["slug"] ?: failSeed("slug is missing")
    if (!Regex("^[a-z0-9-]{1,40}$").matches(slug)) {
        failSeed("slug must be lowercase letters, digits, or hyphen")
    }
    val stickers = mutableListOf<TrialSticker>()
    val codes = mutableSetOf<String>()
    parts[1].lineSequence().forEach { line ->
        val trimmed = line.trim()
        if (trimmed.isEmpty() || trimmed.startsWith("#")) return@forEach
        val pieces = trimmed.split(",", limit = 2)
        val code = pieces[0].trim()
        val name = pieces.getOrNull(1)?.trim()?.ifBlank { null }
        if (!codePattern.matches(code)) {
            failSeed("bad sticker code $code")
        }
        if (!codes.add(code)) {
            failSeed("duplicate sticker code $code")
        }
        if (name != null && name.length > 80) {
            failSeed("sticker name is too long")
        }
        stickers += TrialSticker(code, name)
    }
    if (stickers.isEmpty()) {
        failSeed("sticker list is empty")
    }
    return ParsedSeed(publisher, title, slug, stickers)
}

private fun failSeed(message: String): Nothing {
    log.error("Trial album seed is invalid: {}", message)
    exitProcess(1)
}
