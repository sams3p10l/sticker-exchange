package exchange.domain

import java.util.UUID

enum class StickerStatus(val stored: String) {
    HAVE("have"),
    DUPLICATE("duplicate"),
    WANT("want"),
    ;

    companion object {
        fun fromStored(value: String): StickerStatus = entries.first { it.stored == value }
    }
}

object StickerActions {
    const val CYCLE = "cycle"
    const val INCREASE = "increase"
    const val DECREASE = "decrease"
    const val EMPTY = "empty"
    const val MAX_DUPLICATES = 99
}

data class StickerState(
    val status: StickerStatus,
    val count: Int?,
)

data class AlbumSummary(
    val id: UUID,
    val title: String,
    val publisher: String,
    val slug: String,
)

data class CollectionCard(
    val album: AlbumSummary,
    val owned: Int,
    val total: Int,
)

data class CollectionList(
    val mine: List<CollectionCard>,
    val available: List<AlbumSummary>,
)

data class AlbumSticker(
    val id: UUID,
    val code: String,
    val name: String?,
    val status: String,
    val count: Int?,
)

data class AlbumProgress(
    val total: Int,
    val owned: Int,
    val missing: Int,
    val mineWanted: Int,
    val collectors: Int,
    val offered: Int,
    val wanted: Int,
)

data class AlbumBoard(
    val album: AlbumSummary,
    val inCollection: Boolean,
    val stickers: List<AlbumSticker>,
    val progress: AlbumProgress,
)

data class StickerUpdate(
    val status: String,
    val count: Int?,
    val progress: AlbumProgress,
)

data class StickerSnapshot(
    val state: StickerState?,
    val stickerIds: List<UUID>,
)

interface AlbumRepository {
    fun list(userId: UUID): CollectionList
    fun findBoard(userId: UUID, albumId: UUID): AlbumBoard?
    fun add(userId: UUID, albumId: UUID): Boolean
    fun remove(userId: UUID, albumId: UUID): Boolean
    fun loadSticker(userId: UUID, albumId: UUID, stickerId: UUID): StickerSnapshot?
    fun save(
        userId: UUID,
        albumId: UUID,
        stickerId: UUID,
        stickerIds: List<UUID>,
        state: StickerState?,
    ): AlbumProgress
}
