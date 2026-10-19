package exchange.usecase

import exchange.domain.AlbumCollector
import exchange.domain.AlbumCollectors
import exchange.domain.AlbumProposal
import exchange.domain.AlbumRepository
import exchange.domain.AlbumSummary
import exchange.domain.AppError
import exchange.domain.ProposalContact
import exchange.domain.UserRepository
import exchange.usecase.util.user
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.OffsetDateTime
import java.util.UUID

class ListPartnersUseCaseTest {
    private val users: UserRepository = mockk()
    private val albums: AlbumRepository = mockk()
    private val useCase = ListPartnersUseCase(users, albums)
    private val now = OffsetDateTime.parse("2026-10-19T12:00:00Z")
    private val albumId = UUID.fromString("00000000-0000-0000-0000-0000000000aa")
    private val stickerA = UUID.fromString("00000000-0000-0000-0000-0000000000a1")
    private val stickerB = UUID.fromString("00000000-0000-0000-0000-0000000000b2")
    private val stickerC = UUID.fromString("00000000-0000-0000-0000-0000000000c3")
    private val stickerD = UUID.fromString("00000000-0000-0000-0000-0000000000d4")
    private val viewer = user().copy(inactivityDays = 7)

    @Test
    fun `possible exchange is the smaller of the two directions`() = runTest {
        val partner = collector("mira", wants = setOf(stickerA, stickerC), duplicates = setOf(stickerB))
        stub(viewerDuplicates = setOf(stickerA, stickerC), viewerWants = setOf(stickerB), people = listOf(partner))

        val result = useCase.execute(request())

        val card = result.data.partners.single()
        assertEquals(2, card.iHaveForHim)
        assertEquals(1, card.heHasForMe)
        assertEquals(1, card.possible)
    }

    @Test
    fun `keeps a one sided match and drops a collector with no overlap`() = runTest {
        val oneSided = collector("mira", wants = setOf(stickerA), duplicates = emptySet())
        val none = collector("nema", wants = emptySet(), duplicates = setOf(stickerC))
        stub(
            viewerDuplicates = setOf(stickerA),
            viewerWants = setOf(stickerB),
            people = listOf(oneSided, none),
        )

        val result = useCase.execute(request())

        assertEquals(listOf("mira"), result.data.partners.map { it.username })
        assertEquals(0, result.data.partners.single().possible)
        assertEquals(1, result.data.matchedCount)
    }

    @Test
    fun `sorts by possible exchange then username`() = runTest {
        val lowB = collector("borka", wants = setOf(stickerA), duplicates = setOf(stickerB))
        val lowA = collector("ana", wants = setOf(stickerA), duplicates = setOf(stickerB))
        val high = collector("zora", wants = setOf(stickerA, stickerC), duplicates = setOf(stickerB, stickerD))
        stub(
            viewerDuplicates = setOf(stickerA, stickerC),
            viewerWants = setOf(stickerB, stickerD),
            people = listOf(lowB, high, lowA),
        )

        val result = useCase.execute(request())

        assertEquals(listOf("zora", "ana", "borka"), result.data.partners.map { it.username })
    }

    @Test
    fun `hides someone who has not visited within the threshold`() = runTest {
        val recent = collector("mira", lastSeenAt = now.minusDays(7))
        val old = collector("stara", lastSeenAt = now.minusDays(7).minusSeconds(1))
        val never = collector("nikad", lastSeenAt = null)
        stub(people = listOf(recent, old, never))

        val result = useCase.execute(request())

        assertEquals(listOf("mira"), result.data.partners.map { it.username })
        assertEquals(3, result.data.matchedCount)
        assertEquals(1, result.data.filteredCount)
    }

    @Test
    fun `a threshold of zero shows people who never visited`() = runTest {
        every { users.findById(viewer.id) } returns viewer.copy(inactivityDays = 0)
        val never = collector("nikad", lastSeenAt = null)
        stubPeople(listOf(never))

        val result = useCase.execute(request())

        assertEquals(listOf("nikad"), result.data.partners.map { it.username })
    }

    @Test
    fun `show all keeps inactive people for this visit`() = runTest {
        val old = collector("stara", lastSeenAt = now.minusDays(30))
        stub(people = listOf(old))

        val result = useCase.execute(request(showInactive = true))

        assertEquals(listOf("stara"), result.data.partners.map { it.username })
    }

    @Test
    fun `without proposal hides a partner who already has one`() = runTest {
        val open = collector("mira")
        val free = collector("ana")
        stub(people = listOf(open, free))
        every { albums.proposals(viewer.id, albumId) } returns listOf(
            AlbumProposal(open.userId, ProposalContact.SENT, now.minusDays(1), false),
        )

        val result = useCase.execute(request(withoutProposal = true))

        assertEquals(listOf("ana"), result.data.partners.map { it.username })
    }

    @Test
    fun `returns the second page and clamps a page past the end`() = runTest {
        val people = (1..21).map { collector("u%02d".format(it)) }
        stub(people = people)

        val second = useCase.execute(request(page = 2))
        val past = useCase.execute(request(page = 9))

        assertEquals(1, second.data.partners.size)
        assertEquals("u21", second.data.partners.single().username)
        assertEquals(2, past.data.page)
        assertEquals("u21", past.data.partners.single().username)
    }

    @Test
    fun `returns not found when the album is missing`() = runTest {
        every { users.findById(viewer.id) } returns viewer
        every { albums.collectors(albumId) } returns null

        val result = useCase.execute(request())

        assertTrue(!result.isSuccess)
        assertEquals(AppError.NotFound, result.error)
    }

    private fun request(showInactive: Boolean = false, withoutProposal: Boolean = false, page: Int = 1) = ListPartners(
        userId = viewer.id,
        albumId = albumId,
        showInactive = showInactive,
        withoutProposal = withoutProposal,
        page = page,
        now = now,
    )

    private fun stub(
        viewerDuplicates: Set<UUID> = setOf(stickerA),
        viewerWants: Set<UUID> = setOf(stickerB),
        people: List<AlbumCollector>,
    ) {
        every { users.findById(viewer.id) } returns viewer
        stubPeople(people, viewerDuplicates, viewerWants)
    }

    private fun stubPeople(
        people: List<AlbumCollector>,
        viewerDuplicates: Set<UUID> = setOf(stickerA),
        viewerWants: Set<UUID> = setOf(stickerB),
    ) {
        val self = collector(
            username = viewer.username,
            userId = viewer.id,
            duplicates = viewerDuplicates,
            wants = viewerWants,
        )
        every { albums.collectors(albumId) } returns AlbumCollectors(
            album = AlbumSummary(albumId, "Probni album", "Proba", "trial"),
            people = listOf(self) + people,
        )
        every { albums.proposals(viewer.id, albumId) } returns emptyList()
    }

    private fun collector(
        username: String,
        userId: UUID = UUID.nameUUIDFromBytes(username.toByteArray()),
        wants: Set<UUID> = setOf(stickerA),
        duplicates: Set<UUID> = setOf(stickerB),
        lastSeenAt: OffsetDateTime? = now,
    ) = AlbumCollector(
        userId = userId,
        username = username,
        firstName = username,
        city = "Beograd",
        byMail = true,
        inPerson = false,
        lastSeenAt = lastSeenAt,
        duplicateStickerIds = duplicates,
        wantedStickerIds = wants,
    )
}
