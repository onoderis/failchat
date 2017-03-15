package failchat.exceptions

class UnexpectedResponseCodeException(code: Int) : UnexpectedResponseException(code.toString())
