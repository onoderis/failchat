package failchat.goodgame

import failchat.Origin
import failchat.emoticon.Emoticon

class GgEmoticon(
        code: String,
        url: String
) : Emoticon(Origin.GOODGAME, code, url) {

    var animatedInstance: GgEmoticon? = null

}
