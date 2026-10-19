package exchange.usecase

import exchange.domain.AlbumRepository
import exchange.domain.AppError
import exchange.domain.Result

class RemoveAlbumUseCase(
    private val albums: AlbumRepository,
) : ParameterizedUseCase<OpenAlbum, Unit> {
    override suspend fun execute(params: OpenAlbum): Result<Unit, AppError> {
        if (!albums.remove(params.userId, params.albumId)) return Result.Failure(AppError.NotFound)
        return Result.Success(Unit)
    }
}
