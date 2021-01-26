package failchat.twitch

import com.fasterxml.jackson.annotation.JsonProperty

data class EmoticonSetsResponse(
        @JsonProperty("emoticon_sets")
        val emoticonSets: Map<String, List<EmoticonDto>>
) {

    data class EmoticonDto(
            val code: String,
            val id: Long
    )

}
