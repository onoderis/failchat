package failchat.goodgame

import failchat.core.Origin
import failchat.core.emoticon.Emoticon

class GgEmoticon(
        code: String,
        url: String
) : Emoticon(Origin.goodgame, code, url) {

    var animatedInstance: GgEmoticon? = null

}
