package exchange.usecase

import exchange.domain.AlbumImages
import exchange.domain.AlbumRepository
import exchange.domain.AppError
import exchange.domain.CatalogSticker
import exchange.domain.Result
import exchange.domain.Role
import exchange.domain.UserRepository
import java.util.UUID

private const val PUBLISHER_MAX = 80
private const val TITLE_MAX = 120
private const val STICKER_NAME_MAX = 80
private const val STICKER_LIMIT = 2000
private const val IMAGE_LIMIT = 2 * 1024 * 1024

private val slugPattern = Regex("^[a-z0-9-]{1,40}$")
private val codePattern = Regex("^[A-Za-z0-9]{1,16}$")

data class CreateAlbum(
    val userId: UUID,
    val publisher: String,
    val title: String,
    val slug: String,
    val stickers: String,
    val image: ByteArray?,
)

class CreateAlbumUseCase(
    private val users: UserRepository,
    private val albums: AlbumRepository,
    private val images: AlbumImages,
) : ParameterizedUseCase<CreateAlbum, UUID> {
    override suspend fun execute(params: CreateAlbum): Result<UUID, AppError> {
        val user = users.findById(params.userId) ?: return Result.Failure(AppError.NotFound)
        if (user.role != Role.ADMIN) return Result.Failure(AppError.Forbidden)
        val publisher = params.publisher.trim()
        val title = params.title.trim()
        val requestedSlug = params.slug.trim().lowercase()
        val slug = requestedSlug.ifBlank { slugFrom(title) }
        val parsed = parseStickers(params.stickers)
        val imageExtension = params.image?.let { imageExtension(it) }
        val error = when {
            publisher.isBlank() -> AppError.PublisherMissing
            publisher.length > PUBLISHER_MAX -> AppError.PublisherTooLong
            title.isBlank() -> AppError.TitleMissing
            title.length > TITLE_MAX -> AppError.TitleTooLong
            !slugPattern.matches(slug) -> AppError.SlugInvalid
            parsed is StickerParse.Invalid -> parsed.error
            params.image != null && params.image.size > IMAGE_LIMIT -> AppError.ImageTooLarge
            params.image != null && imageExtension == null -> AppError.ImageUnsupported
            albums.slugExists(slug) -> AppError.SlugTaken
            else -> null
        }
        if (error != null) return Result.Failure(error)
        val stickers = (parsed as StickerParse.Ok).stickers
        val albumId = albums.insertCatalog(publisher, title, slug, stickers)
        if (params.image != null && imageExtension != null) {
            val fileName = images.save(albumId, imageExtension, params.image)
            albums.setImage(albumId, fileName)
        }
        return Result.Success(albumId)
    }
}

private sealed interface StickerParse {
    data class Ok(val stickers: List<CatalogSticker>) : StickerParse
    data class Invalid(val error: AppError) : StickerParse
}

private fun parseStickers(text: String): StickerParse {
    val stickers = mutableListOf<CatalogSticker>()
    val codes = mutableSetOf<String>()
    text.replace("\r\n", "\n").lineSequence().forEach { line ->
        val trimmed = line.trim()
        if (trimmed.isEmpty()) return@forEach
        if (stickers.size >= STICKER_LIMIT) return StickerParse.Invalid(AppError.TooManyStickers)
        val pieces = trimmed.split(",", limit = 2)
        val code = pieces[0].trim()
        val name = pieces.getOrNull(1)?.trim()?.ifBlank { null }
        if (!codePattern.matches(code)) return StickerParse.Invalid(AppError.StickerCodeInvalid)
        if (!codes.add(code)) return StickerParse.Invalid(AppError.StickerCodeDuplicate)
        if (name != null && name.length > STICKER_NAME_MAX) return StickerParse.Invalid(AppError.StickerNameTooLong)
        stickers += CatalogSticker(code, name)
    }
    if (stickers.isEmpty()) return StickerParse.Invalid(AppError.StickersMissing)
    return StickerParse.Ok(stickers)
}

private fun slugFrom(title: String): String {
    val folded = title.lowercase().map { char ->
        when (char) {
            'č', 'ć' -> 'c'
            'ž' -> 'z'
            'š' -> 's'
            'đ' -> 'd'
            else -> char
        }
    }.joinToString("")
    return folded.replace(Regex("[^a-z0-9]+"), "-").trim('-').take(40).trim('-')
}

private fun imageExtension(bytes: ByteArray): String? = when {
    bytes.size >= 3 && bytes[0] == 0xFF.toByte() && bytes[1] == 0xD8.toByte() && bytes[2] == 0xFF.toByte() -> "jpg"
    bytes.size >= 8 && bytes[0] == 0x89.toByte() && bytes[1] == 0x50.toByte() &&
        bytes[2] == 0x4E.toByte() && bytes[3] == 0x47.toByte() -> "png"
    bytes.size >= 12 && bytes.copyOfRange(0, 4).contentEquals("RIFF".toByteArray()) &&
        bytes.copyOfRange(8, 12).contentEquals("WEBP".toByteArray()) -> "webp"
    else -> null
}
