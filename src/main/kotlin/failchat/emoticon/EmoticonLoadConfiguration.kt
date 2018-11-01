package failchat.emoticon

import failchat.Origin

interface EmoticonLoadConfiguration<T: Emoticon> {

    val origin: Origin

    val loadType: LoadType

    /** Loaders for the same set of emoticons. Second and the next loaders in the list is used as backup loaders. */
    val bulkLoaders: List<EmoticonBulkLoader<T>>

    /** Loaders for the same set of emoticons. Second and the next loaders in the list is used as backup loaders. */
    val streamLoaders: List<EmoticonStreamLoader<T>>

    val idExtractor: EmoticonIdExtractor<T>

    enum class LoadType {
        BULK, STREAM
    }
}
