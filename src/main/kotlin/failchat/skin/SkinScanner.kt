package failchat.skin

import java.nio.file.Files
import java.nio.file.Path

class SkinScanner(workDirectory: Path) {

    private val skinsDirectory: Path = workDirectory.resolve("skins")

    fun scan(): List<Skin> {
        // TODO: добавить фильтр для папок где есть <dirname>.html
        Files.newDirectoryStream(skinsDirectory).use { stream ->
            return stream
                    .filterNotNull()
                    .map { Skin(it.fileName.toString(), it) }
        }
    }

}
