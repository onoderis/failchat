package failchat.skin

import java.nio.file.Files
import java.nio.file.Path

class SkinScanner(workDirectory: Path) {

    private val skinsDirectory: Path = workDirectory.resolve("skins")

    fun scan(): List<Skin> {
        Files.newDirectoryStream(skinsDirectory).use { stream ->
            return stream
                    .filterNotNull()
                    .filterNot { it.fileName.toString().startsWith("_") } //ignore _shared directory
                    .map {
                        val skinName = it.fileName.toString()
                        Skin(skinName, resolveSkinPath(skinName))
                    }
                    .filter { Files.exists(it.htmlPath) }
        }
    }

    private fun resolveSkinPath(skinName: String): Path {
        return skinsDirectory.resolve(skinName).resolve("$skinName.html")
    }

}
