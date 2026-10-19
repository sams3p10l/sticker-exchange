package exchange.usecase

import exchange.domain.AppError
import exchange.domain.UserRepository
import exchange.usecase.util.user
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class FindUserUseCaseTest {
    private val users: UserRepository = mockk()
    private val useCase = FindUserUseCase(users)

    @Test
    fun `returns not found when the id is unknown`() = runTest {
        val id = user().id
        every { users.findById(id) } returns null

        val result = useCase.execute(id)

        assertEquals(AppError.NotFound, result.error)
    }

    @Test
    fun `returns the user for a known id`() = runTest {
        val existing = user()
        every { users.findById(existing.id) } returns existing

        val result = useCase.execute(existing.id)

        assertEquals(existing, result.data)
    }
}
