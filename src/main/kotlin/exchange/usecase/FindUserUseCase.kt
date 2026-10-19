package exchange.usecase

import exchange.domain.AppError
import exchange.domain.Result
import exchange.domain.User
import exchange.domain.UserRepository
import java.util.UUID

class FindUserUseCase(
    private val users: UserRepository,
) : ParameterizedUseCase<UUID, User> {
    override suspend fun execute(params: UUID): Result<User, AppError> {
        val user = users.findById(params) ?: return Result.Failure(AppError.NotFound)
        return Result.Success(user)
    }
}
