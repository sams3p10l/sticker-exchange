package exchange.web

import exchange.domain.AlbumBoard
import exchange.domain.AlbumProgress
import exchange.domain.AlbumSticker
import exchange.domain.AlbumSummary
import exchange.domain.CollectionCard
import exchange.domain.StickerActions
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.ApplicationCall
import kotlinx.html.ButtonType
import kotlinx.html.FlowContent
import kotlinx.html.FormEncType
import kotlinx.html.FormMethod
import kotlinx.html.InputType
import kotlinx.html.a
import kotlinx.html.button
import kotlinx.html.div
import kotlinx.html.form
import kotlinx.html.h1
import kotlinx.html.h2
import kotlinx.html.img
import kotlinx.html.input
import kotlinx.html.label
import kotlinx.html.p
import kotlinx.html.textArea
import kotlinx.html.span

suspend fun ApplicationCall.respondCollections(
    mine: List<CollectionCard>,
    available: List<AlbumSummary>,
    admin: Boolean,
) {
    respondLayout(HttpStatusCode.OK, "Moje kolekcije", loggedIn = true, admin = admin, here = "collections") {
        h1 { +"Moje kolekcije" }
        if (mine.isEmpty() && available.isEmpty()) {
            p(classes = "error") { +"Nema albuma za dodavanje." }
        } else if (mine.isEmpty()) {
            p { +"Još nemaš album. Dodaj probni album da bi probao mrežu." }
        } else {
            mine.forEach { card -> albumCard(card) }
        }
        if (available.isNotEmpty()) {
            h2 { +"Dostupni albumi" }
            available.forEach { album ->
                div(classes = "card") {
                    h2 { +album.title }
                    albumCover(album)
                    p(classes = "muted") { +album.publisher }
                    form(action = "/collections/${album.id}", method = FormMethod.post) {
                        button(type = ButtonType.submit) { +"Dodaj" }
                    }
                }
            }
        }
    }
}

suspend fun ApplicationCall.respondAlbum(board: AlbumBoard, admin: Boolean) {
    respondLayout(
        status = HttpStatusCode.OK,
        pageTitle = board.album.title,
        loggedIn = true,
        admin = admin,
        here = "collections",
        wide = true,
        script = if (board.inCollection && board.stickers.isNotEmpty()) "/static/grid.js" else null,
    ) {
        h1 { +board.album.title }
        p(classes = "muted") { +board.album.publisher }
        albumCover(board.album)
        if (!board.inCollection) {
            p { +"Dodaj album da bi označio sličice." }
            form(action = "/collections/${board.album.id}", method = FormMethod.post) {
                button(type = ButtonType.submit) { +"Dodaj" }
            }
            return@respondLayout
        }
        stats(board.progress)
        p { a(classes = "go", href = "/albums/${board.album.id}/partners") { +"Partneri" } }
        p(classes = "hint") { +"Tapni broj: prazno, imam, duplikat, tražim. Kod duplikata plus i minus menjaju broj komada." }
        p(classes = "error") {
            attributes["data-grid-error"] = "true"
            attributes["hidden"] = "hidden"
        }
        legend()
        if (board.stickers.isEmpty()) {
            p { +"Ovaj album nema sličica." }
        } else {
            div(classes = "grid") {
                attributes["data-grid"] = "true"
                board.stickers.forEach { sticker -> stickerCell(board.album.id.toString(), sticker) }
            }
        }
    }
}

suspend fun ApplicationCall.respondRemoveAlbum(album: AlbumSummary, admin: Boolean) {
    respondLayout(HttpStatusCode.OK, "Skini album", loggedIn = true, admin = admin, here = "collections") {
        h1 { +"Skini album" }
        p { +"Skini „${album.title}” sa naloga? Oznake sličica za ovaj album se brišu." }
        form(action = "/collections/${album.id}/remove", method = FormMethod.post) {
            button(type = ButtonType.submit, classes = "danger") { +"Skini" }
        }
        p { a(href = "/collections") { +"Odustani" } }
    }
}

suspend fun ApplicationCall.respondMissingAlbum(admin: Boolean) {
    respondLayout(HttpStatusCode.NotFound, "Album nije pronađen", loggedIn = true, admin = admin) {
        h1 { +"Album nije pronađen" }
        p { +"Nema tog albuma." }
    }
}

private fun FlowContent.albumCard(card: CollectionCard) {
    div(classes = "card") {
        h2 { +card.album.title }
        albumCover(card.album)
        p(classes = "muted") { +card.album.publisher }
        p { +"Imam ${card.owned} od ${card.total}. Fali ${card.total - card.owned}." }
        p(classes = "actions") {
            a(href = "/albums/${card.album.id}") { +"Otvori" }
            a(href = "/albums/${card.album.id}/partners") { +"Partneri" }
        }
        p {
            a(classes = "quiet", href = "/collections/${card.album.id}/remove") { +"Skini sa naloga" }
        }
    }
}

private fun FlowContent.albumCover(album: AlbumSummary) {
    if (album.imagePath != null) {
        img(src = "/albums/${album.id}/cover", alt = album.title, classes = "cover")
    }
}

private fun FlowContent.stats(progress: AlbumProgress) {
    div(classes = "stats") {
    p {
        attributes["data-collectors"] = "true"
        +"Kolekcionara: ${progress.collectors}."
    }
    p {
        attributes["data-offered"] = "true"
        +"Ponuđeno: ${progress.offered}."
    }
    p {
        attributes["data-wanted"] = "true"
        +"Traženo: ${progress.wanted}."
    }
    p {
        attributes["data-missing"] = "true"
        attributes["aria-live"] = "polite"
        +"Fali ${progress.missing} od ${progress.total}."
    }
    p(classes = "muted") {
        attributes["data-owned"] = "true"
        +"Imam ${progress.owned}."
    }
    p(classes = "muted") {
        attributes["data-mine-wanted"] = "true"
        +"Tražim ${progress.mineWanted}."
    }
    }
}

private fun FlowContent.legend() {
    p(classes = "legend") {
        swatch("empty", "Prazno")
        swatch("have", "Imam")
        swatch("duplicate", "Duplikat")
        swatch("want", "Tražim")
    }
}

private fun FlowContent.swatch(status: String, label: String) {
    span(classes = "swatch swatch-$status") { +label }
}

private fun FlowContent.stickerCell(albumId: String, sticker: AlbumSticker) {
    val label = when (sticker.status) {
        "have" -> "${sticker.code}, imam"
        "duplicate" -> "${sticker.code}, duplikat ${sticker.count ?: 1}"
        "want" -> "${sticker.code}, tražim"
        else -> "${sticker.code}, prazno"
    }
    div(classes = "cell") {
        attributes["data-sticker"] = sticker.id.toString()
        attributes["data-album"] = albumId
        attributes["data-code"] = sticker.code
        attributes["data-status"] = sticker.status
        button(type = ButtonType.button, classes = "tap") {
            attributes["data-action"] = StickerActions.CYCLE
            attributes["aria-label"] = label
            if (sticker.name != null) {
                attributes["title"] = sticker.name
            }
            +sticker.code
        }
        div(classes = "adjust") {
            button(type = ButtonType.button, classes = "step") {
                attributes["data-action"] = StickerActions.DECREASE
                attributes["aria-label"] = "Manje"
                +"−"
            }
            span(classes = "count") { +((sticker.count ?: "").toString()) }
            button(type = ButtonType.button, classes = "step") {
                attributes["data-action"] = StickerActions.INCREASE
                attributes["aria-label"] = "Više"
                +"+"
            }
        }
    }
}

suspend fun ApplicationCall.respondNewAlbum(
    error: String? = null,
    publisher: String = "",
    title: String = "",
    slug: String = "",
    stickers: String = "",
) {
    respondLayout(HttpStatusCode.OK, "Dodaj album", loggedIn = true, admin = true, here = "new") {
        h1 { +"Dodaj album" }
        message(error, "error")
        form(action = "/albums/new", method = FormMethod.post, encType = FormEncType.multipartFormData) {
            label {
                +"Izdavač"
                input(type = InputType.text, name = "publisher") {
                    value = publisher
                    maxLength = "80"
                    required = true
                }
            }
            label {
                +"Naslov"
                input(type = InputType.text, name = "title") {
                    value = title
                    maxLength = "120"
                    required = true
                }
            }
            label {
                +"Adresa"
                input(type = InputType.text, name = "slug") {
                    value = slug
                    maxLength = "40"
                }
            }
            p(classes = "muted") { +"Ako adresu ostaviš praznu, pravi se iz naslova." }
            label {
                +"Sličice"
                textArea {
                    name = "stickers"
                    rows = "12"
                    required = true
                    +stickers
                }
            }
            p(classes = "muted") { +"Jedna sličica po redu: šifra ili šifra,ime." }
            label {
                +"Slika albuma"
                input(type = InputType.file, name = "image") {
                    accept = "image/jpeg,image/png,image/webp"
                }
            }
            button(type = ButtonType.submit) { +"Sačuvaj" }
        }
    }
}

suspend fun ApplicationCall.respondForbidden() {
    respondLayout(HttpStatusCode.Forbidden, "Nije dozvoljeno", loggedIn = true) {
        h1 { +"Nije dozvoljeno" }
        p { +"Samo administrator dodaje albume." }
    }
}
