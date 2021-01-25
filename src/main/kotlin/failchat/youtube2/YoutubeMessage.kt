package failchat.youtube2

import failchat.Origin
import failchat.chat.Author
import failchat.chat.ChatMessage

class YoutubeMessage(
        failchatId: Long,
        author: Author,
        text: String
) : ChatMessage(failchatId, Origin.YOUTUBE, author, text)
