package failchat.github

import mu.KLogging
import org.apache.commons.configuration2.Configuration

class ReleaseChecker(
        private val githubClient: GithubClient,
        private val config: Configuration
) {

    private companion object : KLogging() {
        const val lastNotifiedKey = "release-checker.latest-notified-version"
    }

    private val userName: String = config.getString("github.user-name")
    private val repository: String = config.getString("github.repository")

    fun checkNewRelease(onNewRelease: (Release) -> Unit) {
        if (!config.getBoolean("release-checker.enabled")) {
            logger.debug("Release check skipped")
            return
        }

        githubClient
                .requestLatestRelease(userName, repository)
                .thenApply { lastRelease ->
                    val lastNotifiedReleaseVersion = Version.parse(config.getString(lastNotifiedKey))
                    if (lastRelease.version <= lastNotifiedReleaseVersion) {
                        logger.info("Latest version of application installed: '{}'", lastNotifiedReleaseVersion)
                        return@thenApply
                    }

                    config.setProperty(lastNotifiedKey, lastRelease.version.toString())
                    logger.info("Notifying about new release with version: '{}'", lastRelease.version)
                    onNewRelease.invoke(lastRelease)
                }
                .exceptionally { logger.warn("Exception during check for new release", it) }
    }

}
