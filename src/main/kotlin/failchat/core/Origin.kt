package failchat.core

/**
 * Первоисточник сообщений / emoticon'ов. Название сериализуется при отправке сообщений клиентам.
 */
enum class Origin {
    peka2tv,
    goodgame,
    twitch,
    //todo refactor?
    bttvGlobal,
    bttvChannel,
    youtube,
    cybergame,
    failchat,
    test;
}
