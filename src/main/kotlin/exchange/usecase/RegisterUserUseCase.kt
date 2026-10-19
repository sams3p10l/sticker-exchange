package exchange.usecase

import exchange.domain.AppError
import exchange.domain.DEFAULT_INACTIVITY_DAYS
import exchange.domain.InsertedUser
import exchange.domain.NewUser
import exchange.domain.PasswordHasher
import exchange.domain.Result
import exchange.domain.Role
import exchange.domain.User
import exchange.domain.UserRepository

data class RegisterUser(
    val email: String,
    val password: String,
    val username: String,
    val firstName: String,
    val lastName: String,
    val city: String,
    val country: String,
    val byMail: Boolean,
    val inPerson: Boolean,
)

class RegisterUserUseCase(
    private val users: UserRepository,
    private val passwords: PasswordHasher,
) : ParameterizedUseCase<RegisterUser, User> {
    override suspend fun execute(params: RegisterUser): Result<User, AppError> {
        val error = emailError(params.email)
            ?: passwordError(params.password)
            ?: usernameError(params.username)
            ?: personError(params.firstName, params.lastName, params.city, params.country)
        if (error != null) return Result.Failure(error)
        val inserted = users.insert(
            NewUser(
                email = params.email.lowercase(),
                passwordHash = passwords.hash(params.password),
                username = params.username,
                firstName = params.firstName,
                lastName = params.lastName,
                city = params.city,
                country = params.country,
                byMail = params.byMail,
                inPerson = params.inPerson,
                emailNotifications = false,
                inactivityDays = DEFAULT_INACTIVITY_DAYS,
                role = Role.USER,
            ),
        )
        return when (inserted) {
            is InsertedUser.Created -> Result.Success(inserted.user)
            InsertedUser.EmailTaken -> Result.Failure(AppError.EmailTaken)
            InsertedUser.UsernameTaken -> Result.Failure(AppError.UsernameTaken)
        }
    }
}
