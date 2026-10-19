package exchange.usecase

import exchange.domain.AlbumRepository
import exchange.domain.CollectionList
import exchange.usecase.util.user
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ListCollectionsUseCaseTest {
    private val albums: AlbumRepository = mockk()
    private val useCase = ListCollectionsUseCase(albums)

    @Test
    fun `returns the collection from the repository`() = runTest {
        val list = CollectionList(mine = emptyList(), available = emptyList())
        val userId = user().id
        every { albums.list(userId) } returns list

        val result = useCase.execute(userId)

        assertTrue(result.isSuccess)
        assertEquals(list, result.data)
    }
}
