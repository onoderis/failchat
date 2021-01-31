package failchat.twitch

import failchat.chat.MessageHandler
import failchat.util.notEmptyOrNull
import javafx.scene.paint.Color

class TwitchAuthorColorHandler : MessageHandler<TwitchMessage> {

    override fun handleMessage(message: TwitchMessage) {
        val colorString = message.tags.get(TwitchIrcTags.color).notEmptyOrNull() ?: return
        message.author.color = Color.web(colorString)
    }

}
