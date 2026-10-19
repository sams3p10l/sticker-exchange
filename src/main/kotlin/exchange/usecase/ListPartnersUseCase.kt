package exchange.usecase

import exchange.domain.AlbumCollector
import exchange.domain.AlbumProposal
import exchange.domain.AlbumRepository
import exchange.domain.AlbumSummary
import exchange.domain.AppError
import exchange.domain.Result
import exchange.domain.UserRepository
import java.time.OffsetDateTime
import java.util.UUID

const val PARTNER_PAGE_SIZE = 20

data class ListPartners(
    val userId: UUID,
    val albumId: UUID,
    val showInactive: Boolean,
    val withoutProposal: Boolean,
    val page: Int,
    val now: OffsetDateTime,
)

data class PartnerCard(
    val userId: UUID,
    val username: String,
    val firstName: String,
    val city: String,
    val byMail: Boolean,
    val inPerson: Boolean,
    val lastSeenAt: OffsetDateTime?,
    val iHaveForHim: Int,
    val heHasForMe: Int,
    val possible: Int,
    val proposal: AlbumProposal?,
)

data class PartnerPage(
    val album: AlbumSummary,
    val inactivityDays: Int,
    val partners: List<PartnerCard>,
    val page: Int,
    val pageCount: Int,
    val matchedCount: Int,
    val filteredCount: Int,
    val showInactive: Boolean,
    val withoutProposal: Boolean,
)

class ListPartnersUseCase(
    private val users: UserRepository,
    private val albums: AlbumRepository,
) : ParameterizedUseCase<ListPartners, PartnerPage> {
    override suspend fun execute(params: ListPartners): Result<PartnerPage, AppError> {
        val viewer = users.findById(params.userId) ?: return Result.Failure(AppError.NotFound)
        val collected = albums.collectors(params.albumId) ?: return Result.Failure(AppError.NotFound)
        val mine = collected.people.firstOrNull { it.userId == params.userId }
        val proposals = albums.proposals(params.userId, params.albumId).associateBy { it.partnerId }
        val matched = collected.people
            .filter { it.userId != params.userId }
            .mapNotNull { person ->
                card(person, mine, proposals[person.userId])
            }
        val filtered = matched.filter { person ->
            val inactive = inactive(person.lastSeenAt, viewer.inactivityDays, params.now)
            val hiddenByVisit = inactive && !params.showInactive
            val hiddenByProposal = params.withoutProposal && person.proposal != null
            !hiddenByVisit && !hiddenByProposal
        }.sortedWith(compareByDescending<PartnerCard> { it.possible }.thenBy { it.username })
        val pageCount = if (filtered.isEmpty()) 0 else (filtered.size + PARTNER_PAGE_SIZE - 1) / PARTNER_PAGE_SIZE
        val page = if (pageCount == 0) 1 else params.page.coerceIn(1, pageCount)
        val from = (page - 1) * PARTNER_PAGE_SIZE
        return Result.Success(
            PartnerPage(
                album = collected.album,
                inactivityDays = viewer.inactivityDays,
                partners = filtered.drop(from).take(PARTNER_PAGE_SIZE),
                page = page,
                pageCount = pageCount,
                matchedCount = matched.size,
                filteredCount = filtered.size,
                showInactive = params.showInactive,
                withoutProposal = params.withoutProposal,
            ),
        )
    }
}

private fun card(person: AlbumCollector, mine: AlbumCollector?, proposal: AlbumProposal?): PartnerCard? {
    val mineDuplicates = mine?.duplicateStickerIds.orEmpty()
    val mineWants = mine?.wantedStickerIds.orEmpty()
    val iHaveForHim = person.wantedStickerIds.count { it in mineDuplicates }
    val heHasForMe = person.duplicateStickerIds.count { it in mineWants }
    if (iHaveForHim == 0 && heHasForMe == 0) return null
    return PartnerCard(
        userId = person.userId,
        username = person.username,
        firstName = person.firstName,
        city = person.city,
        byMail = person.byMail,
        inPerson = person.inPerson,
        lastSeenAt = person.lastSeenAt,
        iHaveForHim = iHaveForHim,
        heHasForMe = heHasForMe,
        possible = minOf(iHaveForHim, heHasForMe),
        proposal = proposal,
    )
}

private fun inactive(lastSeenAt: OffsetDateTime?, thresholdDays: Int, now: OffsetDateTime): Boolean {
    if (thresholdDays == 0) return false
    if (lastSeenAt == null) return true
    return lastSeenAt.isBefore(now.minusDays(thresholdDays.toLong()))
}
