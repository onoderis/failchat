package failchat.core.viewers

import failchat.core.Origin
import failchat.core.Origin.goodgame
import failchat.core.Origin.peka2tv
import failchat.core.Origin.twitch
import failchat.core.Origin.youtube
import java.util.EnumSet

val countableOrigins: Set<Origin> = EnumSet.of(peka2tv, goodgame, twitch, youtube)
