package failchat

import failchat.emoticon.OriginEmoticonStorageFactory
import org.apache.commons.configuration2.Configuration

fun Configuration.resetEmoticonsUpdatedTime() {
    OriginEmoticonStorageFactory.dbOrigins.forEach {
        this.setProperty(ConfigKeys.lastUpdatedEmoticons(it), 0)
    }
}
