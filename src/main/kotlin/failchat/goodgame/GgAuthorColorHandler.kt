package failchat.goodgame

import failchat.chat.MessageHandler

class GgAuthorColorHandler : MessageHandler<GgMessage> {

    override fun handleMessage(message: GgMessage) {
        val color = GgColors.byRole[message.authorColorName] ?: return
        message.author.color = color
    }
}
