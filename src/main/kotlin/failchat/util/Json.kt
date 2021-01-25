package failchat.util

import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.core.JsonToken
import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.node.JsonNodeFactory
import com.fasterxml.jackson.module.kotlin.KotlinModule

//todo remove duplication
val objectMapper: ObjectMapper = ObjectMapper()
        .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
        .registerModule(KotlinModule())
val nodeFactory: JsonNodeFactory = JsonNodeFactory.instance

/**
 * Read the next token and assert that it is not null.
 * @throws [UnexpectedJsonFormatException] if next token is null.
 * */
fun JsonParser.nextNonNullToken(): JsonToken {
    return nextToken() ?: throw UnexpectedJsonFormatException("Failed to get next token, end of data stream")
}

fun JsonToken.validate(expected: JsonToken): JsonToken {
    if (this != expected) {
        throw UnexpectedJsonFormatException("Expected '$expected' json token, got '$this'")
    }
    return this
}

/**
 * Read next non-null token and assert that it's value is equal to [expected] token. Blocking operation.
 * @throws [UnexpectedJsonFormatException] if next token is null or doesn't equal to [expected] token.
 * */
fun JsonParser.expect(expected: JsonToken): JsonToken {
    return nextNonNullToken().validate(expected)
}

class UnexpectedJsonFormatException : Exception {
    constructor() : super()
    constructor(message: String?) : super(message)
    constructor(message: String?, cause: Throwable?) : super(message, cause)
    constructor(cause: Throwable?) : super(cause)
}

