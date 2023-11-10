package failchat.emoticon

import failchat.Origin

interface EmoticonLoadConfiguration<T: Emoticon> {

    val origin: Origin

    val loader: EmoticonLoader<T>

    val idExtractor: EmoticonIdExtractor<T>
}
