package failchat.experiment

import com.fasterxml.jackson.core.JsonFactory
import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.core.JsonToken
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.node.ObjectNode
import failchat.twitch.TwitchEmoticon
import org.junit.Test
import java.io.IOException
import java.io.PipedInputStream
import java.io.PipedOutputStream
import java.nio.file.Files
import java.nio.file.Paths

class JsonParsing {

    @Test
    fun streamParsingTest() {
        val jsonFile = Paths.get("""docs/integration/twitch/twitch-emoticons-slice.json""")

//        val json = """{"name":"Tom","age":25,"address":["Poland","5th avenue"]}"""
        val jsonFactory = JsonFactory()
        val parser = jsonFactory.createParser(Files.newInputStream(jsonFile))

        val emoticons: MutableList<TwitchEmoticon> = ArrayList()

        parser.nextNonNullToken() // root object
        parser.nextNonNullToken() // 'emoticons' field

        var token = parser.nextNonNullToken() //START_ARRAY

        while (token != JsonToken.END_ARRAY) {
            token = parser.nextNonNullToken() //START_OBJECT

            var id: Long? = null
            var code: String? = null

            while (token != JsonToken.END_OBJECT) {
                parser.nextNonNullToken() // FIELD_NAME
                val fieldName = parser.currentName
                parser.nextNonNullToken()

                when (fieldName) {
                    "id" -> {
                        id = parser.longValue
                    }
                    "code" -> {
                        code = parser.text
                    }
                }
                token = parser.nextNonNullToken() // END_OBJECT
            }

            emoticons.add(TwitchEmoticon(
                    requireNotNull(id) { "'id' not found" },
                    requireNotNull(code) { "'code' not found" },
                    "some/$code"
            ))

            token = parser.nextNonNullToken()
        }

        parser.close()

        emoticons.forEach { println("${it.code}: ${it.twitchId}") }

        /*
        var parsedName: String? = null
        var parsedAge: Int? = null
        val addresses = LinkedList<String>()

        while (parser.nextToken() != JsonToken.END_OBJECT) {
            val fieldname = parser.currentName
            if ("name" == fieldname) {
                parser.nextToken()
                parsedName = parser.text
            }

            if ("age" == fieldname) {
                parser.nextToken()
                parsedAge = parser.intValue
            }

            if ("address" == fieldname) {
                parser.nextToken()
                while (parser.nextToken() !== JsonToken.END_ARRAY) {
                    addresses.add(parser.getText())
                }
            }
        }
        parser.close()
        */
    }

    @Test
    fun streamParsingTestV2() {
        val jsonFile = Paths.get("""docs/integration/twitch/twitch-emoticons-slice.json""")

        val jsonFactory = JsonFactory()
        jsonFactory.codec = ObjectMapper()

        val parser = jsonFactory.createParser(Files.newInputStream(jsonFile))

        val emoticons: MutableList<TwitchEmoticon> = ArrayList()

        parser.nextNonNullToken() // root object
        parser.nextNonNullToken() // 'emoticons' field
        parser.nextNonNullToken() // START_ARRAY

        var token = parser.nextNonNullToken() //START_OBJECT

        while (token != JsonToken.END_ARRAY) {
            val node: ObjectNode = parser.readValueAsTree()

            emoticons.add(TwitchEmoticon(
                    node.get("id").longValue(),
                    node.get("code").textValue(),
                    "some/..."
            ))

            token = parser.nextNonNullToken() //START_OBJECT
        }

        parser.close()

        emoticons.forEach { println("${it.code}: ${it.twitchId}") }
    }


    @Test
    fun noDataAvailableTest() {
        val inStream = PipedInputStream()
        val outStream = PipedOutputStream(inStream)

        outStream.write("""{"name":"value ...""".toByteArray())

        val jsonFactory = JsonFactory()
        val parser = jsonFactory.createParser(inStream)

        while (true) {
            println(parser.nextToken()) //thread will block here
        }
    }
}

fun JsonParser.nextNonNullToken(): JsonToken {
    return nextToken()
            .also { println(it) }
            ?: throw IOException("Failed to get next token, end of data stream")
}

fun JsonToken?.validateNotNull(): JsonToken {
    return this ?: throw IOException("...")
}
