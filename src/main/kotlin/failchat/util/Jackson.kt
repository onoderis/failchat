package failchat.util

import com.fasterxml.jackson.databind.JsonNode

inline fun JsonNode.isEmpty(): Boolean = this.size() == 0
