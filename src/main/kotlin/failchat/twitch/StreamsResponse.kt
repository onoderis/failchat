package failchat.twitch

import com.fasterxml.jackson.annotation.JsonProperty

data class StreamsResponse(
        val data: List<Data>
) {
    data class Data(
            @JsonProperty("viewer_count")
            val viewerCount: Int,
            @JsonProperty("user_login")
            val userLogin: String
    )
}
