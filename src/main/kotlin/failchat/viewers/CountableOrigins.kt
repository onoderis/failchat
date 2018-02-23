package failchat.viewers

import failchat.Origin
import failchat.Origin.GOODGAME
import failchat.Origin.PEKA2TV
import failchat.Origin.TWITCH
import failchat.Origin.YOUTUBE
import java.util.EnumSet

val COUNTABLE_ORIGINS: Set<Origin> = EnumSet.of(PEKA2TV, GOODGAME, TWITCH, YOUTUBE)
