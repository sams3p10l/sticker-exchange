package exchange.usecase

import exchange.domain.AlbumRepository
import exchange.domain.AppError
import exchange.domain.Result
import exchange.domain.StickerActions
import exchange.domain.StickerState
import exchange.domain.StickerStatus
import exchange.domain.StickerUpdate
import java.util.UUID

data class UpdateSticker(
    val userId: UUID,
    val albumId: UUID,
    val stickerId: UUID,
    val action: String,
)

class UpdateStickerUseCase(
    private val albums: AlbumRepository,
) : ParameterizedUseCase<UpdateSticker, StickerUpdate> {
    override suspend fun execute(params: UpdateSticker): Result<StickerUpdate, AppError> {
        val snapshot = albums.loadSticker(params.userId, params.albumId, params.stickerId)
            ?: return Result.Failure(AppError.Rejected)
        val next = nextState(snapshot.state, params.action)
        if (next is Change.Reject) return Result.Failure(AppError.Rejected)
        val stored = when (next) {
            Change.Clear -> null
            is Change.Set -> next.state
            Change.Reject -> null
        }
        val progress = albums.save(
            userId = params.userId,
            albumId = params.albumId,
            stickerId = params.stickerId,
            stickerIds = snapshot.stickerIds,
            state = stored,
        )
        return Result.Success(
            StickerUpdate(
                status = stored?.status?.stored ?: StickerActions.EMPTY,
                count = stored?.count,
                progress = progress,
            ),
        )
    }
}

private sealed interface Change {
    data class Set(val state: StickerState) : Change
    data object Clear : Change
    data object Reject : Change
}

private fun nextState(current: StickerState?, action: String): Change = when (action) {
    StickerActions.CYCLE -> when (current?.status) {
        null -> Change.Set(StickerState(StickerStatus.HAVE, null))
        StickerStatus.HAVE -> Change.Set(StickerState(StickerStatus.DUPLICATE, 1))
        StickerStatus.DUPLICATE -> Change.Set(StickerState(StickerStatus.WANT, null))
        StickerStatus.WANT -> Change.Clear
    }
    StickerActions.INCREASE -> when {
        current == null || current.status != StickerStatus.DUPLICATE -> Change.Reject
        (current.count ?: 1) >= StickerActions.MAX_DUPLICATES -> Change.Set(current)
        else -> Change.Set(current.copy(count = (current.count ?: 1) + 1))
    }
    StickerActions.DECREASE -> when {
        current == null || current.status != StickerStatus.DUPLICATE -> Change.Reject
        (current.count ?: 1) <= 1 -> Change.Set(StickerState(StickerStatus.HAVE, null))
        else -> Change.Set(current.copy(count = (current.count ?: 1) - 1))
    }
    else -> Change.Reject
}
