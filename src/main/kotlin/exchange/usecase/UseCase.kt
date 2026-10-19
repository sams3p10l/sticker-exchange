package exchange.usecase

import exchange.domain.AppError
import exchange.domain.Result

interface ParameterlessUseCase<R> {
    suspend fun execute(): Result<R, AppError>
}

interface ParameterizedUseCase<P, R> {
    suspend fun execute(params: P): Result<R, AppError>
}

interface ActionUseCase {
    suspend fun execute()
}
