package failchat.twitch

import mu.KotlinLogging

val logger = KotlinLogging.logger {}

suspend fun <T> doWithRetryOnAuthError(
        twitchApiClient: TwitchApiClient,
        clientSecret: String,
        tokenContainer: HelixTokenContainer,
        operation: suspend (String) -> T
): T {
    val existingToken = tokenContainer.getToken()

    if (existingToken != null) {
        try {
            return operation(existingToken.value)
        } catch (e: InvalidTokenException) {
            logger.info("Invalid twitch token")
        }
    }

    val newToken = twitchApiClient.generateToken(clientSecret)
    tokenContainer.setToken(newToken)
    return operation(newToken.value)
}
