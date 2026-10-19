package exchange.usecase

import exchange.domain.AppError
import exchange.domain.InsertedUser
import exchange.domain.NewUser
import exchange.domain.PasswordHasher
import exchange.domain.UserRepository
import exchange.usecase.util.registration
import exchange.usecase.util.user
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RegisterUserUseCaseTest {
    private val users: UserRepository = mockk()
    private val passwords: PasswordHasher = mockk()
    private val useCase = RegisterUserUseCase(users, passwords)

    @Test
    fun `rejects a short password before saving`() = runTest {
        val result = useCase.execute(registration(password = "kratka"))

        assertTrue(!result.isSuccess)
        assertEquals(AppError.PasswordTooShort, result.error)
        verify(exactly = 0) { users.insert(any()) }
    }

    @Test
    fun `rejects a password longer than bcrypt allows`() = runTest {
        val result = useCase.execute(registration(password = "a".repeat(73)))

        assertTrue(!result.isSuccess)
        assertEquals(AppError.PasswordTooLong, result.error)
    }

    @Test
    fun `rejects an invalid email`() = runTest {
        val result = useCase.execute(registration().copy(email = "nije-email"))

        assertTrue(!result.isSuccess)
        assertEquals(AppError.InvalidEmail, result.error)
        verify(exactly = 0) { passwords.hash(any()) }
    }

    @Test
    fun `rejects a username outside latin digits and underscore`() = runTest {
        val result = useCase.execute(registration().copy(username = "Ana Ime"))

        assertTrue(!result.isSuccess)
        assertEquals(AppError.InvalidUsername, result.error)
    }

    @Test
    fun `rejects an unknown country`() = runTest {
        val result = useCase.execute(registration().copy(country = "ZZ"))

        assertTrue(!result.isSuccess)
        assertEquals(AppError.CountryInvalid, result.error)
    }

    @Test
    fun `stores the email in lowercase and starts notifications off`() = runTest {
        every { passwords.hash("lozinka1") } returns "hashed"
        val saved = slot<NewUser>()
        every { users.insert(capture(saved)) } returns InsertedUser.Created(user())

        val result = useCase.execute(registration())

        assertTrue(result.isSuccess)
        assertEquals("ana@example.com", saved.captured.email)
        assertEquals("hashed", saved.captured.passwordHash)
        assertTrue(!saved.captured.emailNotifications)
        assertEquals(7, saved.captured.inactivityDays)
        assertTrue(!saved.captured.inPerson)
    }

    @Test
    fun `returns email taken when the address is already used`() = runTest {
        every { passwords.hash(any()) } returns "hashed"
        every { users.insert(any()) } returns InsertedUser.EmailTaken

        val result = useCase.execute(registration())

        assertTrue(!result.isSuccess)
        assertEquals(AppError.EmailTaken, result.error)
    }

    @Test
    fun `returns username taken when the name is already used`() = runTest {
        every { passwords.hash(any()) } returns "hashed"
        every { users.insert(any()) } returns InsertedUser.UsernameTaken

        val result = useCase.execute(registration())

        assertTrue(!result.isSuccess)
        assertEquals(AppError.UsernameTaken, result.error)
    }
}
