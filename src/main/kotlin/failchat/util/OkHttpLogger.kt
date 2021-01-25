package failchat.util

import mu.KotlinLogging
import okhttp3.logging.HttpLoggingInterceptor

object OkHttpLogger : HttpLoggingInterceptor.Logger {

    private val logger = KotlinLogging.logger { }

    override fun log(message: String) {
        logger.info(message)
    }
}
