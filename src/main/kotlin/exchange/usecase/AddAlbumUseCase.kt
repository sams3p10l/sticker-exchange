package exchange.usecase

import exchange.domain.AlbumRepository
import exchange.domain.AppError
import exchange.domain.Result

class AddAlbumUseCase(
    private val albums: AlbumRepository,
) : ParameterizedUseCase<OpenAlbum, Unit> {
    override suspend fun execute(params: OpenAlbum): Result<Unit, AppError> {
        if (!albums.add(params.userId, params.albumId)) return Result.Failure(AppError.NotFound)
        return Result.Success(Unit)
    }
}
