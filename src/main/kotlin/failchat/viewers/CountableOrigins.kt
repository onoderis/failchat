package failchat.viewers

import failchat.Origin
import failchat.Origin.goodgame
import failchat.Origin.peka2tv
import failchat.Origin.twitch
import failchat.Origin.youtube
import java.util.EnumSet

val countableOrigins: Set<Origin> = EnumSet.of(peka2tv, goodgame, twitch, youtube)
