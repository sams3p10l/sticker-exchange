package exchange.usecase

import exchange.domain.AppError
import exchange.domain.PasswordHasher
import exchange.domain.Result
import exchange.domain.User
import exchange.domain.UserRepository

data class LogIn(
    val email: String,
    val password: String,
)

class LogInUseCase(
    private val users: UserRepository,
    private val passwords: PasswordHasher,
) : ParameterizedUseCase<LogIn, User> {
    override suspend fun execute(params: LogIn): Result<User, AppError> {
        val user = users.findByEmail(params.email)
        val hash = user?.let { users.passwordHash(it.id) }
        if (user == null || hash == null || !passwords.verify(params.password, hash)) {
            return Result.Failure(AppError.InvalidCredentials)
        }
        users.recordLogin(user.id)
        return Result.Success(user)
    }
}
