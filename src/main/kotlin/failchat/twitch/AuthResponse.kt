package failchat.twitch

import com.fasterxml.jackson.annotation.JsonProperty

data class AuthResponse(
        @JsonProperty("access_token")
        val accessToken: String,
        @JsonProperty("expires_in")
        val expiresIn: Long
)
