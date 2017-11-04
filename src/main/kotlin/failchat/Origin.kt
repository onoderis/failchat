package failchat

/**
 * Первоисточник сообщений / emoticon'ов. Название сериализуется при отправке сообщений клиентам.
 */
enum class Origin {
    //todo use upper case for names
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
