package exchange.usecase

import exchange.domain.AppError
import exchange.domain.PasswordHasher
import exchange.domain.UserRepository
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.UUID

class ChangePasswordUseCaseTest {
    private val users: UserRepository = mockk()
    private val passwords: PasswordHasher = mockk()
    private val useCase = ChangePasswordUseCase(users, passwords)
    private val userId: UUID = UUID.fromString("00000000-0000-0000-0000-000000000001")

    @Test
    fun `rejects the wrong current password before checking the new one`() = runTest {
        every { users.passwordHash(userId) } returns "hashed"
        every { passwords.verify("pogresna", "hashed") } returns false

        val result = useCase.execute(ChangePassword(userId, "pogresna", "a"))

        assertTrue(!result.isSuccess)
        assertEquals(AppError.WrongPassword, result.error)
        verify(exactly = 0) { users.updatePassword(any(), any()) }
    }

    @Test
    fun `rejects a new password shorter than 8 characters`() = runTest {
        every { users.passwordHash(userId) } returns "hashed"
        every { passwords.verify("stara-lozinka", "hashed") } returns true

        val result = useCase.execute(ChangePassword(userId, "stara-lozinka", "kratka"))

        assertTrue(!result.isSuccess)
        assertEquals(AppError.PasswordTooShort, result.error)
    }

    @Test
    fun `stores the hash of the new password`() = runTest {
        every { users.passwordHash(userId) } returns "hashed"
        every { passwords.verify("stara-lozinka", "hashed") } returns true
        every { passwords.hash("nova-lozinka") } returns "new-hash"
        every { users.updatePassword(userId, "new-hash") } returns true

        val result = useCase.execute(ChangePassword(userId, "stara-lozinka", "nova-lozinka"))

        assertTrue(result.isSuccess)
        verify(exactly = 1) { users.updatePassword(userId, "new-hash") }
    }
}
