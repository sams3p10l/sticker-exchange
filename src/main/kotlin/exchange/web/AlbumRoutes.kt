package exchange.web

import exchange.domain.Result
import exchange.domain.StickerUpdate
import exchange.usecase.AddAlbumUseCase
import exchange.usecase.GetAlbumBoardUseCase
import exchange.usecase.ListCollectionsUseCase
import exchange.usecase.OpenAlbum
import exchange.usecase.RemoveAlbumUseCase
import exchange.usecase.UpdateSticker
import exchange.usecase.UpdateStickerUseCase
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.server.request.receiveParameters
import io.ktor.server.response.respondRedirect
import io.ktor.server.response.respondText
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import org.slf4j.LoggerFactory
import java.util.UUID

private val log = LoggerFactory.getLogger("exchange.album")

fun Route.albumRoutes(
    listCollections: ListCollectionsUseCase,
    getAlbum: GetAlbumBoardUseCase,
    addAlbum: AddAlbumUseCase,
    removeAlbum: RemoveAlbumUseCase,
    updateSticker: UpdateStickerUseCase,
) {
    route("/collections") {
        get {
            val user = call.currentUser()
            if (user == null) {
                call.respondRedirect("/?notice=login_required")
                return@get
            }
            val list = listCollections.execute(user.id)
            call.respondCollections(list.data.mine, list.data.available)
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
                call.respondMissingAlbum()
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
                call.respondMissingAlbum()
                return@get
            }
            call.respondRemoveAlbum(board.data.album)
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
                call.respondMissingAlbum()
                return@post
            }
            log.info("Album {} removed for user {}", albumId, user.id)
            call.respondRedirect("/collections")
        }
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
            call.respondMissingAlbum()
            return@get
        }
        call.respondAlbum(board.data)
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

private fun StickerUpdate.toJson(): String {
    val countJson = count?.toString() ?: "null"
    return """{"status":"$status","count":$countJson,"owned":${progress.owned},"total":${progress.total},"missing":${progress.missing},"mineWanted":${progress.mineWanted},"collectors":${progress.collectors},"offered":${progress.offered},"wanted":${progress.wanted}}"""
}
