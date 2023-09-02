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

fun getFailchatHomePath(): Path {
    return Paths.get(System.getProperty("user.home")).resolve(".failchat")
}
