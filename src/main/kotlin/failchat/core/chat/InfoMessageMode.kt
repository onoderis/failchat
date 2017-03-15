package failchat.core.chat

enum class InfoMessageMode {
    EVERYWHERE {
        override fun toString() = "Everywhere"
    },
    ON_NATIVE_CLIENT {
        override fun toString() = "On native client"
    },
    NOWHERE {
        override fun toString() = "Nowhere"
    };

    companion object {
        @JvmStatic
        fun getValueByString(str: String): InfoMessageMode {
            return InfoMessageMode.valueOf(str.replace(' ', '_').toUpperCase())
        }
    }

}