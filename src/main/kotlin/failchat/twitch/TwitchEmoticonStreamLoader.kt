package failchat.twitch

import failchat.Origin
import failchat.emoticon.EmoticonStreamLoader
import kotlinx.coroutines.experimental.channels.ReceiveChannel

/** Uses official twitch API. */
class TwitchEmoticonStreamLoader(
        private val twitchClient: TwitchApiClient
) : EmoticonStreamLoader<TwitchEmoticon> {

    override val origin = Origin.TWITCH

    override fun loadEmoticons(): ReceiveChannel<TwitchEmoticon> {
        return twitchClient.requestEmoticonsToChannel()
    }

}
