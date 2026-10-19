package exchange.usecase

import exchange.domain.AlbumImages
import exchange.domain.AlbumRepository
import exchange.domain.AppError
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.UUID

class OpenAlbumCoverUseCaseTest {
    private val albums: AlbumRepository = mockk()
    private val images: AlbumImages = mockk()
    private val useCase = OpenAlbumCoverUseCase(albums, images)
    private val albumId = UUID.fromString("00000000-0000-0000-0000-000000000010")

    @Test
    fun `returns not found when the album has no image`() = runTest {
        every { albums.imageName(albumId) } returns null

        val result = useCase.execute(albumId)

        assertEquals(AppError.NotFound, result.error)
    }

    @Test
    fun `returns the stored image`() = runTest {
        val bytes = byteArrayOf(1, 2, 3)
        every { albums.imageName(albumId) } returns "$albumId.png"
        every { images.read("$albumId.png") } returns bytes

        val result = useCase.execute(albumId)

        assertArrayEquals(bytes, result.data?.bytes)
        assertEquals("image/png", result.data?.contentType)
    }
}
