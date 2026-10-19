package exchange.web

import exchange.domain.AlbumProposal
import exchange.domain.ProposalContact
import exchange.usecase.PartnerCard
import exchange.usecase.PartnerPage
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.ApplicationCall
import kotlinx.html.FlowContent
import kotlinx.html.a
import kotlinx.html.div
import kotlinx.html.h1
import kotlinx.html.h2
import kotlinx.html.p
import kotlinx.html.span
import java.time.Duration
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

private val zone = ZoneId.of("Europe/Belgrade")
private val dateTime = DateTimeFormatter.ofPattern("d. M. yyyy. HH:mm", Locale.forLanguageTag("sr-Latn"))
private val dateOnly = DateTimeFormatter.ofPattern("d. M. yyyy.", Locale.forLanguageTag("sr-Latn"))

suspend fun ApplicationCall.respondPartners(page: PartnerPage, now: OffsetDateTime, admin: Boolean) {
    respondLayout(HttpStatusCode.OK, "Partneri", loggedIn = true, admin = admin, here = "collections") {
        h1 { +"Partneri" }
        p(classes = "muted") { +page.album.title }
        p {
            a(href = "/albums/${page.album.id}") { +"Album" }
        }
        filters(page)
        when {
            page.matchedCount == 0 -> p { +"Nema partnera. Fali duplikat ili sličica koju tražiš." }
            page.filteredCount == 0 && page.withoutProposal -> p { +"Nema partnera bez predloga." }
            page.filteredCount == 0 -> p { +"Svi partneri su van praga neaktivnosti." }
            else -> page.partners.forEach { card -> partnerCard(card, now) }
        }
        pager(page)
    }
}

private fun FlowContent.filters(page: PartnerPage) {
    p(classes = "tools") {
        a(href = partnersHref(page, showInactive = page.showInactive, withoutProposal = !page.withoutProposal, pageNumber = 1)) {
            +if (page.withoutProposal) "Prikaži i sa predlogom" else "Bez predloga"
        }
        if (page.inactivityDays > 0) {
            a(
                href = partnersHref(
                    page,
                    showInactive = !page.showInactive,
                    withoutProposal = page.withoutProposal,
                    pageNumber = 1,
                ),
            ) {
                +if (page.showInactive) "Sakrij neaktivne" else "Pokaži sve"
            }
        }
    }
    p(classes = "hint") {
        +when {
            page.inactivityDays == 0 -> "Prag neaktivnosti je isključen."
            page.showInactive -> "Prikazani su i neaktivni."
            else -> "Sakriveni su koji nisu ulazili ${page.inactivityDays} dana i koji nikad nisu ušli."
        }
    }
}

private fun FlowContent.partnerCard(card: PartnerCard, now: OffsetDateTime) {
    div(classes = "card") {
        h2 {
            a(href = "/users/${card.username}") { +card.username }
        }
        p { +"${card.firstName}, ${card.city}" }
        p(classes = "muted") { +exchangeMethod(card.byMail, card.inPerson) }
        p(classes = "muted") { +lastSeenText(card.lastSeenAt, now) }
        div(classes = "figures") {
            figure("Imam za njega", card.iHaveForHim)
            figure("Ima za mene", card.heHasForMe)
            figure("Moguća razmena", card.possible)
        }
        p { +proposalText(card.proposal) }
    }
}

private fun FlowContent.figure(label: String, value: Int) {
    div {
        span { +value.toString() }
        span { +label }
    }
}

private fun FlowContent.pager(page: PartnerPage) {
    if (page.pageCount <= 1) return
    p(classes = "tools") {
        if (page.page > 1) {
            a(href = partnersHref(page, page.showInactive, page.withoutProposal, page.page - 1)) { +"Prethodna" }
        }
        span { +"Strana ${page.page} od ${page.pageCount}" }
        if (page.page < page.pageCount) {
            a(href = partnersHref(page, page.showInactive, page.withoutProposal, page.page + 1)) { +"Sledeća" }
        }
    }
}

private fun partnersHref(listed: PartnerPage, showInactive: Boolean, withoutProposal: Boolean, pageNumber: Int): String {
    val params = mutableListOf<String>()
    if (pageNumber > 1) params += "page=$pageNumber"
    if (showInactive) params += "inactive=all"
    if (withoutProposal) params += "proposals=none"
    val query = if (params.isEmpty()) "" else "?" + params.joinToString("&")
    return "/albums/${listed.album.id}/partners$query"
}

private fun exchangeMethod(byMail: Boolean, inPerson: Boolean): String = when {
    byMail && inPerson -> "Pošta i uživo"
    byMail -> "Pošta"
    inPerson -> "Uživo"
    else -> "Nije naveo način"
}

private fun lastSeenText(at: OffsetDateTime?, now: OffsetDateTime): String {
    if (at == null) return "Još nema ulaska."
    val minutes = Duration.between(at, now).toMinutes()
    return when {
        minutes < 1 -> "Upravo"
        minutes < 60 -> "Pre $minutes min"
        minutes < 60 * 24 -> "Pre ${hours(minutes / 60)}"
        minutes < 60 * 24 * 7 -> "Pre ${days(minutes / (60 * 24))}"
        else -> dateTime.format(at.atZoneSameInstant(zone))
    }
}

private fun hours(value: Long): String = when (value) {
    1L -> "1 sat"
    in 2L..4L -> "$value sata"
    else -> "$value sati"
}

private fun days(value: Long): String = when (value) {
    1L -> "1 dan"
    else -> "$value dana"
}

private fun proposalText(proposal: AlbumProposal?): String {
    if (proposal == null) return "Nema predloga"
    val dated = dateOnly.format(proposal.at.atZoneSameInstant(zone))
    val base = when (proposal.contact) {
        ProposalContact.SENT -> "Poslato $dated"
        ProposalContact.RECEIVED -> "Stigao predlog $dated"
        ProposalContact.AGREED -> "Dogovoreno $dated"
        ProposalContact.REJECTED -> "Odbijeno $dated"
        ProposalContact.EXPIRED -> "Isteklo $dated"
        ProposalContact.CANCELLED -> "Otkazano $dated"
    }
    val visit = when (proposal.partnerVisitedAfter) {
        true -> " Bio na sajtu posle predloga."
        false -> " Nije ulazio otkad si poslao."
        null -> ""
    }
    return base + visit
}
