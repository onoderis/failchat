package failchat.emoticon

import failchat.ConfigKeys
import failchat.Origin.BTTV_CHANNEL
import failchat.Origin.BTTV_GLOBAL
import failchat.Origin.FAILCHAT
import failchat.Origin.FRANKERFASEZ
import failchat.Origin.GOODGAME
import failchat.Origin.PEKA2TV
import failchat.Origin.TWITCH
import failchat.chat.DeletedMessagePlaceholder
import failchat.chat.Elements
import org.apache.commons.configuration2.Configuration

class DeletedMessagePlaceholderFactory(
        private val emoticonFinder: EmoticonFinder,
        private val config: Configuration
) {

    private val prefixes = mapOf(
            "tw" to TWITCH,
            "gg" to GOODGAME,
            "pk" to PEKA2TV,
            "fc" to FAILCHAT,
            "btg" to BTTV_GLOBAL,
            "btc" to BTTV_CHANNEL,
            "ffz" to FRANKERFASEZ
    )

    fun create(): DeletedMessagePlaceholder {
        val text = config.getString(ConfigKeys.deletedMessagePlaceholder)
        val emoticons = ArrayList<Emoticon>(2)

        val escapedText = text
                .let { Elements.escapeBraces(it) }
                .let { Elements.escapeLabelCharacters(it) }

        val processedText = SemicolonCodeProcessor.process(escapedText) { code ->
            val prefixAndCode = code.split("-", ignoreCase = true, limit = 2)
            val origin = prefixes[prefixAndCode.first()]
                    ?: return@process ReplaceDecision.Skip

            val emoticon = emoticonFinder.findByCode(origin, prefixAndCode[1])
                    ?: return@process ReplaceDecision.Skip

            val emoticonNo = emoticons.size
            val label = Elements.label(emoticonNo)
            emoticons.add(emoticon)

            ReplaceDecision.Replace(label)
        }

        return DeletedMessagePlaceholder(processedText, emoticons)
    }

}
