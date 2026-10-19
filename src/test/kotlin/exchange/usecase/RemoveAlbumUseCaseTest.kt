package exchange.usecase

import exchange.domain.AlbumRepository
import exchange.domain.AppError
import exchange.usecase.util.user
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.UUID

class RemoveAlbumUseCaseTest {
    private val albums: AlbumRepository = mockk()
    private val useCase = RemoveAlbumUseCase(albums)
    private val open = OpenAlbum(user().id, UUID.fromString("00000000-0000-0000-0000-000000000002"))

    @Test
    fun `returns not found when the album does not exist`() = runTest {
        every { albums.remove(open.userId, open.albumId) } returns false

        val result = useCase.execute(open)

        assertEquals(AppError.NotFound, result.error)
    }
}
