package failchat.exception

class UnexpectedResponseCodeException(code: Int) : UnexpectedResponseException(code.toString())
