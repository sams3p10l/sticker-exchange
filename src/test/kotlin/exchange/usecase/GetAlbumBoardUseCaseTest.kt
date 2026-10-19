package exchange.usecase

import exchange.domain.AlbumBoard
import exchange.domain.AlbumProgress
import exchange.domain.AlbumRepository
import exchange.domain.AlbumSummary
import exchange.domain.AppError
import exchange.usecase.util.user
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.UUID

class GetAlbumBoardUseCaseTest {
    private val albums: AlbumRepository = mockk()
    private val useCase = GetAlbumBoardUseCase(albums)
    private val open = OpenAlbum(user().id, UUID.fromString("00000000-0000-0000-0000-000000000002"))

    @Test
    fun `returns not found when the album is missing`() = runTest {
        every { albums.findBoard(open.userId, open.albumId) } returns null

        val result = useCase.execute(open)

        assertEquals(AppError.NotFound, result.error)
    }

    @Test
    fun `returns the board when the album exists`() = runTest {
        val board = AlbumBoard(
            album = AlbumSummary(open.albumId, "Probni album", "Proba", "trial"),
            inCollection = false,
            stickers = emptyList(),
            progress = AlbumProgress(0, 0, 0, 0, 0, 0, 0),
        )
        every { albums.findBoard(open.userId, open.albumId) } returns board

        val result = useCase.execute(open)

        assertEquals(board, result.data)
    }
}
