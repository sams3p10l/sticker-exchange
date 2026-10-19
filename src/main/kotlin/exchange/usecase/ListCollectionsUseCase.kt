package exchange.usecase

import exchange.domain.AlbumRepository
import exchange.domain.AppError
import exchange.domain.CollectionList
import exchange.domain.Result
import java.util.UUID

class ListCollectionsUseCase(
    private val albums: AlbumRepository,
) : ParameterizedUseCase<UUID, CollectionList> {
    override suspend fun execute(params: UUID): Result<CollectionList, AppError> =
        Result.Success(albums.list(params))
}
