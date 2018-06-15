package failchat.chat.badge

interface BadgeFinder {
    fun findBadge(origin: BadgeOrigin, badgeId: Any): Badge?
}