package failchat.chat

import org.apache.commons.configuration2.Configuration

class AppConfiguration(
        val config: Configuration
) {
    @Volatile
    var deletedMessagePlaceholder = DeletedMessagePlaceholder("message deleted", listOf())
}
