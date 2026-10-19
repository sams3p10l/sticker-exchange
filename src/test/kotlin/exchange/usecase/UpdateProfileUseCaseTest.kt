package exchange.usecase

import exchange.domain.AppError
import exchange.domain.ProfileUpdate
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

class UpdateProfileUseCaseTest {
    private val users: UserRepository = mockk()
    private val useCase = UpdateProfileUseCase(users)
    private val userId: UUID = user().id

    @Test
    fun `rejects a negative inactivity threshold`() = runTest {
        val result = useCase.execute(profile(inactivityDays = "-1"))

        assertTrue(!result.isSuccess)
        assertEquals(AppError.InactivityInvalid, result.error)
        verify(exactly = 0) { users.updateProfile(any(), any()) }
    }

    @Test
    fun `stores a blank about as no text`() = runTest {
        val saved = slot<ProfileUpdate>()
        every { users.updateProfile(userId, capture(saved)) } returns user()

        val result = useCase.execute(profile(about = "   ", inactivityDays = "0"))

        assertTrue(result.isSuccess)
        assertEquals(null, saved.captured.about)
        assertEquals(0, saved.captured.inactivityDays)
    }

    @Test
    fun `returns not found when the user row is gone`() = runTest {
        every { users.updateProfile(userId, any()) } returns null

        val result = useCase.execute(profile())

        assertTrue(!result.isSuccess)
        assertEquals(AppError.NotFound, result.error)
    }

    private fun profile(about: String = "Zdravo", inactivityDays: String = "7") = UpdateProfile(
        userId = userId,
        firstName = "Ana",
        lastName = "Anic",
        city = "Beograd",
        country = "RS",
        about = about,
        byMail = true,
        inPerson = true,
        emailNotifications = true,
        inactivityDays = inactivityDays,
    )
}
