package exchange.storage

import exchange.domain.AlbumImages
import java.io.File
import java.util.UUID

class LocalAlbumImages(
    private val root: File = File("data/album-images"),
) : AlbumImages {
    override fun save(albumId: UUID, extension: String, bytes: ByteArray): String {
        root.mkdirs()
        val fileName = "$albumId.$extension"
        File(root, fileName).writeBytes(bytes)
        return fileName
    }

    override fun read(fileName: String): ByteArray? {
        if (!fileName.matches(FILE_NAME)) return null
        val file = File(root, fileName)
        if (!file.isFile) return null
        return file.readBytes()
    }

    private companion object {
        val FILE_NAME = Regex("^[0-9a-fA-F-]{36}\\.(jpg|png|webp)$")
    }
}
