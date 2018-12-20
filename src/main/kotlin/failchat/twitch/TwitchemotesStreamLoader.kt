package failchat.twitch

import failchat.Origin
import failchat.emoticon.EmoticonStreamLoader
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.channels.toChannel
import kotlinx.coroutines.launch

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
