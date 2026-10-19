package exchange.persistence

internal interface Mapper<I, O> {
    fun map(input: I): O
}
