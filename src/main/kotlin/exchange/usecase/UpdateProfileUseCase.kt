package exchange.usecase

import exchange.domain.AppError
import exchange.domain.ProfileUpdate
import exchange.domain.Result
import exchange.domain.User
import exchange.domain.UserRepository
import java.util.UUID

data class UpdateProfile(
    val userId: UUID,
    val firstName: String,
    val lastName: String,
    val city: String,
    val country: String,
    val about: String,
    val byMail: Boolean,
    val inPerson: Boolean,
    val emailNotifications: Boolean,
    val inactivityDays: String,
)

class UpdateProfileUseCase(
    private val users: UserRepository,
) : ParameterizedUseCase<UpdateProfile, User> {
    override suspend fun execute(params: UpdateProfile): Result<User, AppError> {
        val inactivityDays = params.inactivityDays.toIntOrNull()
        val fieldError = personError(params.firstName, params.lastName, params.city, params.country)
        val error = when {
            fieldError != null -> fieldError
            params.about.length > ABOUT_MAX -> AppError.AboutTooLong
            inactivityDays == null || inactivityDays < 0 -> AppError.InactivityInvalid
            else -> null
        }
        if (error != null || inactivityDays == null) return Result.Failure(error ?: AppError.InactivityInvalid)
        val updated = users.updateProfile(
            params.userId,
            ProfileUpdate(
                firstName = params.firstName,
                lastName = params.lastName,
                city = params.city,
                country = params.country,
                about = params.about.ifBlank { null },
                byMail = params.byMail,
                inPerson = params.inPerson,
                emailNotifications = params.emailNotifications,
                inactivityDays = inactivityDays,
            ),
        ) ?: return Result.Failure(AppError.NotFound)
        return Result.Success(updated)
    }
}
