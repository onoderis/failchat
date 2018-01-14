package failchat.util

import com.fasterxml.jackson.databind.JsonNode

fun JsonNode.isEmpty(): Boolean = this.size() == 0
