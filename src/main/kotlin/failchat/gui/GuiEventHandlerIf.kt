package failchat.gui

interface GuiEventHandler {

    fun handleStartChat()

    fun handleStopChat()

    fun handleShutDown()

    fun handleResetUserConfiguration()

    fun handleConfigurationChange()

    fun handleClearChat()

    fun notifyEmoticonsAreLoading()

    fun notifyEmoticonsLoaded()

}

