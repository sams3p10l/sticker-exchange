package exchange.usecase

import exchange.domain.AlbumImages
import exchange.domain.AlbumRepository
import exchange.domain.AppError
import exchange.domain.Result
import java.util.UUID

data class AlbumCover(
    val bytes: ByteArray,
    val contentType: String,
)

class OpenAlbumCoverUseCase(
    private val albums: AlbumRepository,
    private val images: AlbumImages,
) : ParameterizedUseCase<UUID, AlbumCover> {
    override suspend fun execute(params: UUID): Result<AlbumCover, AppError> {
        val fileName = albums.imageName(params) ?: return Result.Failure(AppError.NotFound)
        val bytes = images.read(fileName) ?: return Result.Failure(AppError.NotFound)
        val contentType = when {
            fileName.endsWith(".png") -> "image/png"
            fileName.endsWith(".webp") -> "image/webp"
            else -> "image/jpeg"
        }
        return Result.Success(AlbumCover(bytes, contentType))
    }
}
