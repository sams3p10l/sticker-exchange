package exchange.usecase

import exchange.domain.AppError
import exchange.domain.Result
import exchange.domain.User
import exchange.domain.UserRepository

class FindUserByUsernameUseCase(
    private val users: UserRepository,
) : ParameterizedUseCase<String, User> {
    override suspend fun execute(params: String): Result<User, AppError> {
        val user = users.findByUsername(params) ?: return Result.Failure(AppError.NotFound)
        return Result.Success(user)
    }
}
