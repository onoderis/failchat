package failchat.twitch

import com.fasterxml.jackson.annotation.JsonProperty

data class SevenTvChannelResponse(
        @JsonProperty("emote_set")
        val emoteSet: SevenTvEmoteSetResponse
)
