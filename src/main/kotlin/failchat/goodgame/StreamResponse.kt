package failchat.goodgame

import com.fasterxml.jackson.annotation.JsonProperty

data class StreamResponse(
        val status: String,
        @JsonProperty("player_viewers")
        val playerViewers: Int
)
