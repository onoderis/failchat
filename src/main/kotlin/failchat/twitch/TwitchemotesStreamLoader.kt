package failchat.twitch

import failchat.Origin
import failchat.emoticon.EmoticonStreamLoader
import kotlinx.coroutines.experimental.GlobalScope
import kotlinx.coroutines.experimental.channels.Channel
import kotlinx.coroutines.experimental.channels.ReceiveChannel
import kotlinx.coroutines.experimental.channels.toChannel
import kotlinx.coroutines.experimental.launch

/** Uses twitchemotes.com API. */
class TwitchemotesStreamLoader(
        private val twitchemotesApiClient: TwitchemotesApiClient
) : EmoticonStreamLoader<TwitchEmoticon> {

    override val origin = Origin.TWITCH

    override fun loadEmoticons(): ReceiveChannel<TwitchEmoticon> {
        val resultChannel = Channel<TwitchEmoticon>(Channel.UNLIMITED)
        GlobalScope.launch {
            try {
                twitchemotesApiClient.requestGlobalEmoticonsToChannel().toChannel(resultChannel)
                twitchemotesApiClient.requestAllEmoticonsToChannel().toChannel(resultChannel)
                resultChannel.close()
            } catch (t: Throwable) {
                resultChannel.close(t)
            }
        }

        return resultChannel
    }
}
