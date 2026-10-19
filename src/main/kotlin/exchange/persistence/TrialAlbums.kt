package exchange.persistence

data class TrialSticker(val code: String, val name: String?)

interface TrialAlbums {
    fun exists(slug: String): Boolean
    fun insert(publisher: String, title: String, slug: String, stickers: List<TrialSticker>)
}
