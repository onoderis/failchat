package failchat.twitch

import com.fasterxml.jackson.annotation.JsonProperty

data class BadgesResponse(
        val data: List<Data>
) {

    data class Data(
            @JsonProperty("set_id")
            val setId: String,
            val versions: List<Version>
    )

    data class Version(
            val id: String,
            @JsonProperty("image_url_1x")
            val imageUrl1x: String,
            val description: String
    )
}
