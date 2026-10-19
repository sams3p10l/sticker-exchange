package exchange.usecase

import exchange.domain.AlbumRepository
import exchange.domain.AppError
import exchange.domain.StickerActions
import exchange.domain.StickerSnapshot
import exchange.domain.StickerState
import exchange.domain.StickerStatus
import exchange.usecase.util.progress
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.UUID

class UpdateStickerUseCaseTest {
    private val albums: AlbumRepository = mockk()
    private val useCase = UpdateStickerUseCase(albums)
    private val userId: UUID = UUID.fromString("00000000-0000-0000-0000-000000000001")
    private val albumId: UUID = UUID.fromString("00000000-0000-0000-0000-000000000002")
    private val stickerId: UUID = UUID.fromString("00000000-0000-0000-0000-000000000003")
    private val stickerIds = listOf(stickerId)

    @Test
    fun `cycles an empty sticker to have`() = runTest {
        stub(current = null)

        val result = useCase.execute(action(StickerActions.CYCLE))

        assertTrue(result.isSuccess)
        assertEquals("have", result.data.status)
        assertEquals(null, result.data.count)
        verifySaved(StickerState(StickerStatus.HAVE, null))
    }

    @Test
    fun `cycles have to a duplicate of one`() = runTest {
        stub(StickerState(StickerStatus.HAVE, null))

        val result = useCase.execute(action(StickerActions.CYCLE))

        assertEquals("duplicate", result.data.status)
        assertEquals(1, result.data.count)
    }

    @Test
    fun `cycles a duplicate to want`() = runTest {
        stub(StickerState(StickerStatus.DUPLICATE, 2))

        val result = useCase.execute(action(StickerActions.CYCLE))

        assertEquals("want", result.data.status)
        assertEquals(null, result.data.count)
    }

    @Test
    fun `cycles want back to empty`() = runTest {
        stub(StickerState(StickerStatus.WANT, null))

        val result = useCase.execute(action(StickerActions.CYCLE))

        assertEquals("empty", result.data.status)
        verifySaved(null)
    }

    @Test
    fun `rejects plus when the sticker is not a duplicate`() = runTest {
        every { albums.loadSticker(userId, albumId, stickerId) } returns StickerSnapshot(
            StickerState(StickerStatus.HAVE, null),
            stickerIds,
        )

        val result = useCase.execute(action(StickerActions.INCREASE))

        assertTrue(!result.isSuccess)
        assertEquals(AppError.Rejected, result.error)
        verify(exactly = 0) { albums.save(any(), any(), any(), any(), any()) }
    }

    @Test
    fun `increases a duplicate by one`() = runTest {
        stub(StickerState(StickerStatus.DUPLICATE, 2))

        val result = useCase.execute(action(StickerActions.INCREASE))

        assertEquals(3, result.data.count)
        verifySaved(StickerState(StickerStatus.DUPLICATE, 3))
    }

    @Test
    fun `stops a duplicate at 99`() = runTest {
        stub(StickerState(StickerStatus.DUPLICATE, 99))

        useCase.execute(action(StickerActions.INCREASE))

        verifySaved(StickerState(StickerStatus.DUPLICATE, 99))
    }

    @Test
    fun `minus at one returns the sticker to have`() = runTest {
        stub(StickerState(StickerStatus.DUPLICATE, 1))

        val result = useCase.execute(action(StickerActions.DECREASE))

        assertEquals("have", result.data.status)
        assertEquals(null, result.data.count)
    }

    @Test
    fun `minus above one keeps the duplicate`() = runTest {
        stub(StickerState(StickerStatus.DUPLICATE, 4))

        useCase.execute(action(StickerActions.DECREASE))

        verifySaved(StickerState(StickerStatus.DUPLICATE, 3))
    }

    @Test
    fun `rejects an unknown action`() = runTest {
        every { albums.loadSticker(userId, albumId, stickerId) } returns StickerSnapshot(null, stickerIds)

        val result = useCase.execute(action("paint"))

        assertEquals(AppError.Rejected, result.error)
        verify(exactly = 0) { albums.save(any(), any(), any(), any(), any()) }
    }

    @Test
    fun `rejects a sticker that is not in the collection`() = runTest {
        every { albums.loadSticker(userId, albumId, stickerId) } returns null

        val result = useCase.execute(action(StickerActions.CYCLE))

        assertEquals(AppError.Rejected, result.error)
    }

    private fun action(name: String) = UpdateSticker(userId, albumId, stickerId, name)

    private fun stub(current: StickerState?) {
        every { albums.loadSticker(userId, albumId, stickerId) } returns StickerSnapshot(current, stickerIds)
        every { albums.save(userId, albumId, stickerId, stickerIds, any()) } returns progress()
    }

    private fun verifySaved(state: StickerState?) {
        verify { albums.save(userId, albumId, stickerId, stickerIds, state) }
    }
}
