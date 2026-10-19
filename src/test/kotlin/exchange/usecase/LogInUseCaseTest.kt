package exchange.usecase

import exchange.domain.AppError
import exchange.domain.PasswordHasher
import exchange.domain.UserRepository
import exchange.usecase.util.user
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LogInUseCaseTest {
    private val users: UserRepository = mockk()
    private val passwords: PasswordHasher = mockk()
    private val useCase = LogInUseCase(users, passwords)
    private val existing = user()

    @Test
    fun `returns the same error when the email is unknown`() = runTest {
        every { users.findByEmail("ana@example.com") } returns null

        val result = useCase.execute(LogIn("ana@example.com", "lozinka1"))

        assertTrue(!result.isSuccess)
        assertEquals(AppError.InvalidCredentials, result.error)
        verify(exactly = 0) { users.recordLogin(any()) }
    }

    @Test
    fun `returns the same error when the password is wrong`() = runTest {
        every { users.findByEmail("ana@example.com") } returns existing
        every { users.passwordHash(existing.id) } returns "hashed"
        every { passwords.verify("pogresna", "hashed") } returns false

        val result = useCase.execute(LogIn("ana@example.com", "pogresna"))

        assertTrue(!result.isSuccess)
        assertEquals(AppError.InvalidCredentials, result.error)
        verify(exactly = 0) { users.recordLogin(any()) }
    }

    @Test
    fun `signs in with the username when the field has no at sign`() = runTest {
        every { users.findByUsername("ana") } returns existing
        every { users.passwordHash(existing.id) } returns "hashed"
        every { passwords.verify("lozinka1", "hashed") } returns true
        every { users.recordLogin(existing.id) } returns Unit

        val result = useCase.execute(LogIn("ana", "lozinka1"))

        assertTrue(result.isSuccess)
        assertEquals(existing.id, result.data.id)
        verify(exactly = 0) { users.findByEmail(any()) }
    }

    @Test
    fun `records the visit when the password matches`() = runTest {
        every { users.findByEmail("ana@example.com") } returns existing
        every { users.passwordHash(existing.id) } returns "hashed"
        every { passwords.verify("lozinka1", "hashed") } returns true
        every { users.recordLogin(existing.id) } returns Unit

        val result = useCase.execute(LogIn("ana@example.com", "lozinka1"))

        assertTrue(result.isSuccess)
        assertEquals(existing, result.data)
        verify(exactly = 1) { users.recordLogin(existing.id) }
    }
}
