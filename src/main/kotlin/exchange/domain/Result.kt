package exchange.domain

sealed class Result<out T, out E> {
    data class Success<T>(val value: T) : Result<T, Nothing>()
    data class Failure<E>(val reason: E) : Result<Nothing, E>()

    val isSuccess: Boolean get() = this is Success

    val data: T
        get() = (this as Success).value

    val error: E
        get() = (this as Failure).reason
}
