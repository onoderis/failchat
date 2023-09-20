package failchat

import failchat.emoticon.OriginEmoticonStorageFactory
import org.apache.commons.configuration2.Configuration
import java.nio.file.Path
import java.nio.file.Paths

fun Configuration.resetEmoticonsUpdatedTime() {
    OriginEmoticonStorageFactory.dbOrigins.forEach {
        this.setProperty(ConfigKeys.lastUpdatedEmoticons(it), 0)
    }
}

val workingDirectory: Path = Paths.get("")
val failchatHomePath: Path = Paths.get(System.getProperty("user.home")).resolve(".failchat")
val failchatEmoticonsDirectory: Path = failchatHomePath.resolve("failchat-emoticons")
val emoticonCacheDirectory: Path = workingDirectory.resolve("emoticons")
val emoticonDbFile: Path = emoticonCacheDirectory.resolve("emoticons.db")

val failchatEmoticonsUrl = "http://${FailchatServerInfo.host.hostAddress}:${FailchatServerInfo.port}/emoticons/"
