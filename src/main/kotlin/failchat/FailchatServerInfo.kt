package failchat

import java.net.InetAddress

object FailchatServerInfo {
    val host: InetAddress = InetAddress.getLoopbackAddress()
    const val defaultPort = 10880
    var port = defaultPort
}
