package exchange.persistence

import exchange.domain.AlbumBoard
import exchange.domain.AlbumProgress
import exchange.domain.AlbumRepository
import exchange.domain.AlbumSticker
import exchange.domain.AlbumSummary
import exchange.domain.CollectionCard
import exchange.domain.CollectionList
import exchange.domain.StickerActions
import exchange.domain.StickerSnapshot
import exchange.domain.StickerState
import exchange.domain.StickerStatus
import org.jetbrains.exposed.v1.core.SortOrder
import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.inList
import org.jetbrains.exposed.v1.core.java.javaUUID
import org.jetbrains.exposed.v1.javatime.timestampWithTimeZone
import org.jetbrains.exposed.v1.jdbc.deleteWhere
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.jetbrains.exposed.v1.jdbc.update
import java.time.OffsetDateTime
import java.util.UUID

internal object Publishers : Table("publishers") {
    val id = javaUUID("id")
    val name = varchar("name", 80)
    override val primaryKey = PrimaryKey(id)
}

internal object Albums : Table("albums") {
    val id = javaUUID("id")
    val publisherId = javaUUID("publisher_id")
    val slug = varchar("slug", 40)
    val title = varchar("title", 120)
    override val primaryKey = PrimaryKey(id)
}

internal object Stickers : Table("stickers") {
    val id = javaUUID("id")
    val albumId = javaUUID("album_id")
    val code = varchar("code", 16)
    val sortOrder = integer("sort_order")
    val name = varchar("name", 80).nullable()
    override val primaryKey = PrimaryKey(id)
}

internal object CollectionAlbums : Table("collection_albums") {
    val userId = javaUUID("user_id")
    val albumId = javaUUID("album_id")
    val addedAt = timestampWithTimeZone("added_at")
    override val primaryKey = PrimaryKey(userId, albumId)
}

internal object StickerStates : Table("sticker_states") {
    val userId = javaUUID("user_id")
    val stickerId = javaUUID("sticker_id")
    val status = varchar("status", 16)
    val duplicateCount = integer("duplicate_count").nullable()
    override val primaryKey = PrimaryKey(userId, stickerId)
}

class ExposedAlbumRepository : AlbumRepository, TrialAlbums {
    override fun list(userId: UUID): CollectionList = transaction {
        val albums = loadAlbums()
        val mine = CollectionAlbums
            .selectAll()
            .where { CollectionAlbums.userId eq userId }
            .map { it[CollectionAlbums.albumId] }
            .toSet()
        val cards = albums.filter { it.id in mine }.map { album ->
            val stickers = loadStickers(album.id)
            val states = loadStates(userId, stickers.map { it.id })
            val owned = states.values.count { it.status == StickerStatus.HAVE || it.status == StickerStatus.DUPLICATE }
            CollectionCard(album, owned, stickers.size)
        }
        CollectionList(mine = cards, available = albums.filter { it.id !in mine })
    }

    override fun findBoard(userId: UUID, albumId: UUID): AlbumBoard? = transaction {
        val album = loadAlbum(albumId) ?: return@transaction null
        val stickers = loadStickers(albumId)
        val inCollection = ownsAlbum(userId, albumId)
        val states = if (inCollection) loadStates(userId, stickers.map { it.id }) else emptyMap()
        val views = stickers.map { sticker ->
            val state = states[sticker.id]
            AlbumSticker(
                id = sticker.id,
                code = sticker.code,
                name = sticker.name,
                status = state?.status?.stored ?: StickerActions.EMPTY,
                count = state?.count,
            )
        }
        AlbumBoard(
            album = album,
            inCollection = inCollection,
            stickers = views,
            progress = progressFor(userId, albumId, stickers.map { it.id }),
        )
    }

    override fun add(userId: UUID, albumId: UUID): Boolean = transaction {
        if (loadAlbum(albumId) == null) return@transaction false
        if (!ownsAlbum(userId, albumId)) {
            CollectionAlbums.insert {
                it[CollectionAlbums.userId] = userId
                it[CollectionAlbums.albumId] = albumId
                it[addedAt] = OffsetDateTime.now()
            }
        }
        true
    }

    override fun remove(userId: UUID, albumId: UUID): Boolean = transaction {
        if (loadAlbum(albumId) == null) return@transaction false
        val stickerIds = loadStickers(albumId).map { it.id }
        if (stickerIds.isNotEmpty()) {
            StickerStates.deleteWhere {
                (StickerStates.userId eq userId) and (StickerStates.stickerId inList stickerIds)
            }
        }
        CollectionAlbums.deleteWhere {
            (CollectionAlbums.userId eq userId) and (CollectionAlbums.albumId eq albumId)
        }
        true
    }

    override fun loadSticker(userId: UUID, albumId: UUID, stickerId: UUID): StickerSnapshot? = transaction {
        if (!ownsAlbum(userId, albumId)) return@transaction null
        val stickers = loadStickers(albumId)
        if (stickers.none { it.id == stickerId }) return@transaction null
        StickerSnapshot(
            state = loadStates(userId, listOf(stickerId))[stickerId],
            stickerIds = stickers.map { it.id },
        )
    }

    override fun save(
        userId: UUID,
        albumId: UUID,
        stickerId: UUID,
        stickerIds: List<UUID>,
        state: StickerState?,
    ): AlbumProgress = transaction {
        writeState(userId, stickerId, state)
        progressFor(userId, albumId, stickerIds)
    }

    override fun exists(slug: String): Boolean = transaction {
        Albums.selectAll().where { Albums.slug eq slug }.limit(1).firstOrNull() != null
    }

    override fun insert(publisher: String, title: String, slug: String, stickers: List<TrialSticker>) {
        transaction {
            val publisherId = UUID.randomUUID()
            val albumId = UUID.randomUUID()
            Publishers.insert {
                it[id] = publisherId
                it[name] = publisher
            }
            Albums.insert {
                it[id] = albumId
                it[Albums.publisherId] = publisherId
                it[Albums.slug] = slug
                it[Albums.title] = title
            }
            stickers.forEachIndexed { index, sticker ->
                Stickers.insert {
                    it[id] = UUID.randomUUID()
                    it[Stickers.albumId] = albumId
                    it[code] = sticker.code
                    it[sortOrder] = index + 1
                    it[name] = sticker.name
                }
            }
        }
    }
}

private data class StickerRow(val id: UUID, val code: String, val name: String?, val sortOrder: Int)

private fun loadAlbums(): List<AlbumSummary> {
    val publishers = Publishers.selectAll().associate { it[Publishers.id] to it[Publishers.name] }
    return Albums.selectAll()
        .orderBy(Albums.title to SortOrder.ASC)
        .map { row ->
            AlbumSummary(
                id = row[Albums.id],
                title = row[Albums.title],
                publisher = publishers[row[Albums.publisherId]].orEmpty(),
                slug = row[Albums.slug],
            )
        }
}

private fun loadAlbum(albumId: UUID): AlbumSummary? = loadAlbums().firstOrNull { it.id == albumId }

private fun loadStickers(albumId: UUID): List<StickerRow> =
    Stickers.selectAll()
        .where { Stickers.albumId eq albumId }
        .orderBy(Stickers.sortOrder to SortOrder.ASC)
        .map { row ->
            StickerRow(
                id = row[Stickers.id],
                code = row[Stickers.code],
                name = row[Stickers.name],
                sortOrder = row[Stickers.sortOrder],
            )
        }

private fun loadStates(userId: UUID, stickerIds: List<UUID>): Map<UUID, StickerState> {
    if (stickerIds.isEmpty()) return emptyMap()
    return StickerStates.selectAll()
        .where { (StickerStates.userId eq userId) and (StickerStates.stickerId inList stickerIds) }
        .associate { row ->
            row[StickerStates.stickerId] to StickerState(
                status = StickerStatus.fromStored(row[StickerStates.status]),
                count = row[StickerStates.duplicateCount],
            )
        }
}

private fun ownsAlbum(userId: UUID, albumId: UUID): Boolean =
    CollectionAlbums.selectAll().where {
        (CollectionAlbums.userId eq userId) and (CollectionAlbums.albumId eq albumId)
    }.limit(1).firstOrNull() != null

private fun progressFor(userId: UUID, albumId: UUID, stickerIds: List<UUID>): AlbumProgress {
    val mine = loadStates(userId, stickerIds)
    val owned = mine.values.count { it.status == StickerStatus.HAVE || it.status == StickerStatus.DUPLICATE }
    val mineWanted = mine.values.count { it.status == StickerStatus.WANT }
    val all = if (stickerIds.isEmpty()) {
        emptyList()
    } else {
        StickerStates.selectAll().where { StickerStates.stickerId inList stickerIds }.toList()
    }
    val offered = all.filter { it[StickerStates.status] == StickerStatus.DUPLICATE.stored }
        .sumOf { it[StickerStates.duplicateCount] ?: 0 }
    val wanted = all.count { it[StickerStates.status] == StickerStatus.WANT.stored }
    val collectors = CollectionAlbums.selectAll().where { CollectionAlbums.albumId eq albumId }.count().toInt()
    return AlbumProgress(
        total = stickerIds.size,
        owned = owned,
        missing = stickerIds.size - owned,
        mineWanted = mineWanted,
        collectors = collectors,
        offered = offered,
        wanted = wanted,
    )
}

private fun writeState(userId: UUID, stickerId: UUID, next: StickerState?) {
    val exists = StickerStates.selectAll().where {
        (StickerStates.userId eq userId) and (StickerStates.stickerId eq stickerId)
    }.limit(1).firstOrNull() != null
    if (next == null) {
        if (exists) {
            StickerStates.deleteWhere {
                (StickerStates.userId eq userId) and (StickerStates.stickerId eq stickerId)
            }
        }
        return
    }
    if (exists) {
        StickerStates.update({
            (StickerStates.userId eq userId) and (StickerStates.stickerId eq stickerId)
        }) {
            it[status] = next.status.stored
            it[duplicateCount] = next.count
        }
    } else {
        StickerStates.insert {
            it[StickerStates.userId] = userId
            it[StickerStates.stickerId] = stickerId
            it[status] = next.status.stored
            it[duplicateCount] = next.count
        }
    }
}
