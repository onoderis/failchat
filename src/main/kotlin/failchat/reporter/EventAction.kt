package failchat.reporter

enum class EventAction(val queryParamValue: String) {
    APP_LAUNCH("AppLaunch"),
    HEARTBEAT("Heartbeat")
}
