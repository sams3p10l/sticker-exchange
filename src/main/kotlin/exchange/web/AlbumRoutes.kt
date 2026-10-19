package exchange.web

import exchange.domain.Role
import exchange.domain.Result
import exchange.domain.StickerUpdate
import exchange.usecase.AddAlbumUseCase
import exchange.usecase.CreateAlbum
import exchange.usecase.CreateAlbumUseCase
import exchange.usecase.GetAlbumBoardUseCase
import exchange.usecase.ListCollectionsUseCase
import exchange.usecase.ListPartners
import exchange.usecase.ListPartnersUseCase
import exchange.usecase.OpenAlbum
import exchange.usecase.OpenAlbumCoverUseCase
import exchange.usecase.RemoveAlbumUseCase
import exchange.usecase.UpdateSticker
import exchange.usecase.UpdateStickerUseCase
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.content.PartData
import io.ktor.http.content.forEachPart
import io.ktor.server.request.receiveMultipart
import io.ktor.server.request.receiveParameters
import io.ktor.server.response.respondBytes
import io.ktor.server.response.respondRedirect
import io.ktor.server.response.respondText
import io.ktor.utils.io.readBuffer
import kotlinx.io.readByteArray
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import org.slf4j.LoggerFactory
import java.time.OffsetDateTime
import java.util.UUID

private val log = LoggerFactory.getLogger("exchange.album")

fun Route.albumRoutes(
    listCollections: ListCollectionsUseCase,
    getAlbum: GetAlbumBoardUseCase,
    addAlbum: AddAlbumUseCase,
    removeAlbum: RemoveAlbumUseCase,
    updateSticker: UpdateStickerUseCase,
    listPartners: ListPartnersUseCase,
    createAlbum: CreateAlbumUseCase,
    openCover: OpenAlbumCoverUseCase,
) {
    route("/collections") {
        get {
            val user = call.currentUser()
            if (user == null) {
                call.respondRedirect("/?notice=login_required")
                return@get
            }
            val list = listCollections.execute(user.id)
            call.respondCollections(list.data.mine, list.data.available, user.role == Role.ADMIN)
        }
        post("/{albumId}") {
            val user = call.currentUser()
            if (user == null) {
                call.respondRedirect("/?notice=login_required")
                return@post
            }
            val albumId = call.albumId()
            val added = albumId?.let { addAlbum.execute(OpenAlbum(user.id, it)) }
            if (added !is Result.Success) {
                call.respondMissingAlbum(user.role == Role.ADMIN)
                return@post
            }
            log.info("Album {} added for user {}", albumId, user.id)
            call.respondRedirect("/albums/$albumId")
        }
        get("/{albumId}/remove") {
            val user = call.currentUser()
            if (user == null) {
                call.respondRedirect("/?notice=login_required")
                return@get
            }
            val albumId = call.albumId()
            val board = albumId?.let { getAlbum.execute(OpenAlbum(user.id, it)) }
            if (board !is Result.Success || !board.data.inCollection) {
                call.respondMissingAlbum(user.role == Role.ADMIN)
                return@get
            }
            call.respondRemoveAlbum(board.data.album, user.role == Role.ADMIN)
        }
        post("/{albumId}/remove") {
            val user = call.currentUser()
            if (user == null) {
                call.respondRedirect("/?notice=login_required")
                return@post
            }
            val albumId = call.albumId()
            val removed = albumId?.let { removeAlbum.execute(OpenAlbum(user.id, it)) }
            if (removed !is Result.Success) {
                call.respondMissingAlbum(user.role == Role.ADMIN)
                return@post
            }
            log.info("Album {} removed for user {}", albumId, user.id)
            call.respondRedirect("/collections")
        }
    }

    get("/albums/{albumId}/partners") {
        val user = call.currentUser()
        if (user == null) {
            call.respondRedirect("/?notice=login_required")
            return@get
        }
        val albumId = call.albumId()
        if (albumId == null) {
            call.respondMissingAlbum(user.role == Role.ADMIN)
            return@get
        }
        val now = OffsetDateTime.now()
        val query = call.request.queryParameters
        when (
            val listed = listPartners.execute(
                ListPartners(
                    userId = user.id,
                    albumId = albumId,
                    showInactive = query["inactive"] == "all",
                    withoutProposal = query["proposals"] == "none",
                    page = query["page"]?.toIntOrNull() ?: 1,
                    now = now,
                ),
            )
        ) {
            is Result.Success -> call.respondPartners(listed.data, now, user.role == Role.ADMIN)
            is Result.Failure -> call.respondMissingAlbum(user.role == Role.ADMIN)
        }
    }

    get("/albums/new") {
        val user = call.currentUser()
        if (user == null) {
            call.respondRedirect("/?notice=login_required")
            return@get
        }
        if (user.role != Role.ADMIN) {
            call.respondForbidden()
            return@get
        }
        call.respondNewAlbum()
    }

    post("/albums/new") {
        val user = call.currentUser()
        if (user == null) {
            call.respondRedirect("/?notice=login_required")
            return@post
        }
        if (user.role != Role.ADMIN) {
            call.respondForbidden()
            return@post
        }
        val form = call.readAlbumForm()
        when (
            val created = createAlbum.execute(
                CreateAlbum(
                    userId = user.id,
                    publisher = form.publisher,
                    title = form.title,
                    slug = form.slug,
                    stickers = form.stickers,
                    image = form.image,
                ),
            )
        ) {
            is Result.Success -> {
                log.info("Album {} created", created.data)
                call.respondRedirect("/albums/${created.data}")
            }
            is Result.Failure -> call.respondNewAlbum(
                error = created.error.message(),
                publisher = form.publisher,
                title = form.title,
                slug = form.slug,
                stickers = form.stickers,
            )
        }
    }

    get("/albums/{albumId}/cover") {
        val albumId = call.albumId()
        val cover = albumId?.let { openCover.execute(it) }
        if (cover !is Result.Success) {
            call.respondText("Nema slike.", status = HttpStatusCode.NotFound)
            return@get
        }
        call.respondBytes(cover.data.bytes, ContentType.parse(cover.data.contentType))
    }

    get("/albums/{albumId}") {
        val user = call.currentUser()
        if (user == null) {
            call.respondRedirect("/?notice=login_required")
            return@get
        }
        val albumId = call.albumId()
        val board = albumId?.let { getAlbum.execute(OpenAlbum(user.id, it)) }
        if (board !is Result.Success) {
            call.respondMissingAlbum(user.role == Role.ADMIN)
            return@get
        }
        call.respondAlbum(board.data, user.role == Role.ADMIN)
    }

    post("/albums/{albumId}/stickers/{stickerId}") {
        val user = call.currentUser()
        if (user == null) {
            call.respondText(errorJson(), ContentType.Application.Json, HttpStatusCode.Unauthorized)
            return@post
        }
        val albumId = call.albumId()
        val stickerId = call.parameters["stickerId"]?.toUuidOrNull()
        if (albumId == null || stickerId == null) {
            call.respondText(errorJson(), ContentType.Application.Json, HttpStatusCode.NotFound)
            return@post
        }
        val action = call.receiveParameters()["action"].orEmpty()
        when (val update = updateSticker.execute(UpdateSticker(user.id, albumId, stickerId, action))) {
            is Result.Success -> call.respondText(update.data.toJson(), ContentType.Application.Json)
            is Result.Failure -> call.respondText(
                errorJson(),
                ContentType.Application.Json,
                HttpStatusCode.BadRequest,
            )
        }
    }
}

private fun io.ktor.server.application.ApplicationCall.albumId(): UUID? =
    parameters["albumId"]?.toUuidOrNull()

private fun String.toUuidOrNull(): UUID? = runCatching { UUID.fromString(this) }.getOrNull()

private fun errorJson(): String = """{"error":"rejected"}"""

private data class AlbumForm(
    val publisher: String,
    val title: String,
    val slug: String,
    val stickers: String,
    val image: ByteArray?,
)

private suspend fun io.ktor.server.application.ApplicationCall.readAlbumForm(): AlbumForm {
    val fields = mutableMapOf<String, String>()
    var image: ByteArray? = null
    receiveMultipart().forEachPart { part ->
        when (part) {
            is PartData.FormItem -> fields[part.name.orEmpty()] = part.value
            is PartData.FileItem -> {
                val packet = part.provider().readBuffer(2_097_153L)
                val bytes = packet.readByteArray()
                if (bytes.isNotEmpty()) image = bytes
            }
            else -> Unit
        }
        part.release()
    }
    return AlbumForm(
        publisher = fields["publisher"].orEmpty(),
        title = fields["title"].orEmpty(),
        slug = fields["slug"].orEmpty(),
        stickers = fields["stickers"].orEmpty(),
        image = image,
    )
}

private fun StickerUpdate.toJson(): String {
    val countJson = count?.toString() ?: "null"
    return """{"status":"$status","count":$countJson,"owned":${progress.owned},"total":${progress.total},"missing":${progress.missing},"mineWanted":${progress.mineWanted},"collectors":${progress.collectors},"offered":${progress.offered},"wanted":${progress.wanted}}"""
}
