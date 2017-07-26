package failchat.github

import org.apache.commons.configuration2.Configuration
import org.slf4j.Logger
import org.slf4j.LoggerFactory

class ReleaseChecker(
        private val githubClient: GithubClient,
        private val config: Configuration
) {

    private companion object {
        val log: Logger = LoggerFactory.getLogger(ReleaseChecker::class.java)
        val lastNotifiedKey = "update-checker.latest-notified-version"
    }

    fun checkNewRelease(onNewRelease: (Release) -> Unit) {
        if (!config.getBoolean("update-checker.enabled")) {
            log.debug("Release check skipped")
            return
        }

        githubClient
                .requestLatestRelease("sph-u", "failchat")
                .thenApply { lastRelease ->
                    val lastNotifiedReleaseVersion = Version.parse(config.getString(lastNotifiedKey))
                    if (lastRelease.version <= lastNotifiedReleaseVersion) {
                        log.info("Latest version of application installed: '{}'", lastNotifiedReleaseVersion)
                        return@thenApply
                    }

                    config.setProperty(lastNotifiedKey, lastRelease.version.toString())
                    log.info("Notifying about new release with version: '{}'", lastRelease.version)
                    onNewRelease.invoke(lastRelease)
                }
                .exceptionally { log.warn("Exception during check for new release", it) }
    }

}
