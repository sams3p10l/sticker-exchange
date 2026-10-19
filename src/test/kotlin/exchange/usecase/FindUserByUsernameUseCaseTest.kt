package exchange.usecase

import exchange.domain.AppError
import exchange.domain.UserRepository
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class FindUserByUsernameUseCaseTest {
    private val users: UserRepository = mockk()
    private val useCase = FindUserByUsernameUseCase(users)

    @Test
    fun `returns not found when the username is unknown`() = runTest {
        every { users.findByUsername("ana") } returns null

        val result = useCase.execute("ana")

        assertEquals(AppError.NotFound, result.error)
    }
}
