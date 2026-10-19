package exchange.usecase

import exchange.domain.AlbumBoard
import exchange.domain.AlbumRepository
import exchange.domain.AppError
import exchange.domain.Result
import java.util.UUID

data class OpenAlbum(
    val userId: UUID,
    val albumId: UUID,
)

class GetAlbumBoardUseCase(
    private val albums: AlbumRepository,
) : ParameterizedUseCase<OpenAlbum, AlbumBoard> {
    override suspend fun execute(params: OpenAlbum): Result<AlbumBoard, AppError> {
        val board = albums.findBoard(params.userId, params.albumId) ?: return Result.Failure(AppError.NotFound)
        return Result.Success(board)
    }
}
