package failchat.chat.handlers

import failchat.chat.ChatMessage
import failchat.chat.Elements
import failchat.chat.Image
import failchat.chat.Link
import failchat.chat.MessageHandler
import failchat.emoticon.Emoticon
import mu.KLogging
import java.io.BufferedWriter
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

class ChatHistoryLogger(private val chatHistoryWriter: BufferedWriter) : MessageHandler<ChatMessage> {

    private companion object : KLogging() {
        val dateTimeFormatter: DateTimeFormatter = DateTimeFormatter.ISO_OFFSET_DATE_TIME
        val zone: ZoneId = ZoneId.systemDefault()
    }

    override fun handleMessage(message: ChatMessage) {
        val textualMessage = message.elements.foldIndexed(message.text) { index, text, element ->
            val replacement: String = when (element) {
                is Emoticon -> element.code
                is Link -> element.fullUrl
                is Image -> element.link.fullUrl
                else -> {
                    logger.error("Unknown element type: {}", element.javaClass.name)
                    ""
                }
            }

            text.replace(Elements.label(index), replacement)
        }

        val time = message.timestamp.truncatedTo(ChronoUnit.SECONDS).atZone(zone)
        chatHistoryWriter.appendLine("${dateTimeFormatter.format(time)} [${message.origin.commonName}] " +
                "${message.author.name}: $textualMessage")
        chatHistoryWriter.flush()
    }

}
