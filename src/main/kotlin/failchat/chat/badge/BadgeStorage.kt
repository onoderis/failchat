package failchat.chat.badge

import java.util.concurrent.ConcurrentHashMap

class BadgeStorage : BadgeFinder {

    private val badgesMap: MutableMap<BadgeOrigin, Map<out Any, Badge>> = ConcurrentHashMap()

    override fun findBadge(origin: BadgeOrigin, badgeId: Any): Badge? {
        return badgesMap.get(origin)?.get(badgeId)
    }

    fun putBadges(origin: BadgeOrigin, badges: Map<out Any, Badge>) {
        badgesMap.put(origin, badges)
    }

}