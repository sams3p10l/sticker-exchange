package exchange.usecase

import exchange.domain.AppError
import exchange.domain.PasswordHasher
import exchange.domain.Result
import exchange.domain.UserRepository
import java.util.UUID

data class ChangePassword(
    val userId: UUID,
    val currentPassword: String,
    val newPassword: String,
)

class ChangePasswordUseCase(
    private val users: UserRepository,
    private val passwords: PasswordHasher,
) : ParameterizedUseCase<ChangePassword, Unit> {
    override suspend fun execute(params: ChangePassword): Result<Unit, AppError> {
        val hash = users.passwordHash(params.userId)
        if (hash == null || !passwords.verify(params.currentPassword, hash)) {
            return Result.Failure(AppError.WrongPassword)
        }
        val error = passwordError(params.newPassword)
        if (error != null) return Result.Failure(error)
        if (!users.updatePassword(params.userId, passwords.hash(params.newPassword))) {
            return Result.Failure(AppError.NotFound)
        }
        return Result.Success(Unit)
    }
}
