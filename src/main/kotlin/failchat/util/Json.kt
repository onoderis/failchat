package failchat.util

import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.core.JsonToken
import com.fasterxml.jackson.databind.ObjectMapper

val objectMapper = ObjectMapper()

fun JsonParser.nextNonNullToken(): JsonToken {
    return nextToken() ?: throw UnexpectedJsonFormatException("Failed to get next token, end of data stream")
}

fun JsonToken.validate(expected: JsonToken): JsonToken {
    if (this != expected) {
        throw UnexpectedJsonFormatException("Expected '$expected' json token, got '$this'")
    }
    return this
}

class UnexpectedJsonFormatException : Exception {
    constructor() : super()
    constructor(message: String?) : super(message)
    constructor(message: String?, cause: Throwable?) : super(message, cause)
    constructor(cause: Throwable?) : super(cause)
}

