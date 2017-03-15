package failchat.goodgame

import failchat.core.chat.MessageHandler

import java.util.regex.Pattern

class HtmlUrlCleaner : MessageHandler<GgMessage> {

    private val htmlUrlPattern = Pattern.compile("""<a target="_blank" rel="nofollow" href="(.*)">\1</a>""")

    override fun handleMessage(message: GgMessage) {
        var matcher = htmlUrlPattern.matcher(message.text)
        while (matcher.find()) {
            val url = matcher.group(1)
            message.text = matcher.replaceFirst(url)
            matcher = htmlUrlPattern.matcher(message.text)
        }
    }
}
