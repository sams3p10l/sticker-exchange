package exchange.usecase

import exchange.domain.AlbumImages
import exchange.domain.AlbumRepository
import exchange.domain.AppError
import exchange.domain.CatalogSticker
import exchange.domain.Role
import exchange.domain.UserRepository
import exchange.usecase.util.user
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.UUID

class CreateAlbumUseCaseTest {
    private val users: UserRepository = mockk()
    private val albums: AlbumRepository = mockk()
    private val images: AlbumImages = mockk()
    private val useCase = CreateAlbumUseCase(users, albums, images)
    private val admin = user().copy(role = Role.ADMIN)

    @Test
    fun `rejects a user who is not an administrator`() = runTest {
        every { users.findById(admin.id) } returns user()

        val result = useCase.execute(draft())

        assertEquals(AppError.Forbidden, result.error)
        verify(exactly = 0) { albums.insertCatalog(any(), any(), any(), any()) }
    }

    @Test
    fun `rejects an empty sticker list`() = runTest {
        every { users.findById(admin.id) } returns admin

        val result = useCase.execute(draft(stickers = "\n  \n"))

        assertEquals(AppError.StickersMissing, result.error)
    }

    @Test
    fun `rejects a repeated sticker code`() = runTest {
        every { users.findById(admin.id) } returns admin

        val result = useCase.execute(draft(stickers = "1\n1"))

        assertEquals(AppError.StickerCodeDuplicate, result.error)
    }

    @Test
    fun `rejects a slug that is not a short address`() = runTest {
        every { users.findById(admin.id) } returns admin

        val result = useCase.execute(draft(slug = "Los naslov"))

        assertEquals(AppError.SlugInvalid, result.error)
    }

    @Test
    fun `builds the slug from the title when the address is blank`() = runTest {
        every { users.findById(admin.id) } returns admin
        every { albums.slugExists("sampioni-cuda") } returns false
        val saved = slot<List<CatalogSticker>>()
        every { albums.insertCatalog("Panini", "Šampioni čuda", "sampioni-cuda", capture(saved)) } returns albumId

        val result = useCase.execute(draft(title = "Šampioni čuda", slug = "  ", stickers = "1,Igrač\nB1"))

        assertEquals(albumId, result.data)
        assertEquals(listOf(CatalogSticker("1", "Igrač"), CatalogSticker("B1", null)), saved.captured)
    }

    @Test
    fun `rejects a slug that is already used`() = runTest {
        every { users.findById(admin.id) } returns admin
        every { albums.slugExists("proba") } returns true

        val result = useCase.execute(draft(slug = "proba"))

        assertEquals(AppError.SlugTaken, result.error)
    }

    @Test
    fun `stores the image after the album is created`() = runTest {
        every { users.findById(admin.id) } returns admin
        every { albums.slugExists("proba") } returns false
        every { albums.insertCatalog(any(), any(), any(), any()) } returns albumId
        every { images.save(albumId, "jpg", jpeg) } returns "$albumId.jpg"
        every { albums.setImage(albumId, "$albumId.jpg") } returns Unit

        val result = useCase.execute(draft(image = jpeg))

        assertTrue(result.isSuccess)
        verify { albums.setImage(albumId, "$albumId.jpg") }
    }

    private fun draft(
        title: String = "Probni album",
        slug: String = "proba",
        stickers: String = "1",
        image: ByteArray? = null,
    ) = CreateAlbum(
        userId = admin.id,
        publisher = "Panini",
        title = title,
        slug = slug,
        stickers = stickers,
        image = image,
    )

    private companion object {
        val albumId: UUID = UUID.fromString("00000000-0000-0000-0000-000000000010")
        val jpeg = byteArrayOf(0xFF.toByte(), 0xD8.toByte(), 0xFF.toByte(), 0x00)
    }
}
