package failchat.exception

class UnexpectedResponseCodeException : UnexpectedResponseException{
    constructor(code: Int) : super("code: '$code'")
    constructor(code: Int, url: String) : super("code: '$code', url: '$url'")
}
