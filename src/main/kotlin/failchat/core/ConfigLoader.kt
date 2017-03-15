package failchat.core

import org.apache.commons.configuration.CompositeConfiguration
import org.apache.commons.configuration.PropertiesConfiguration
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.nio.file.Files
import java.nio.file.Path

/**
 * Загружает и сохраняет конфигурацию.
 * */
class ConfigLoader(workDir: Path) {

    private companion object {
        val log: Logger = LoggerFactory.getLogger(ConfigLoader::class.java)
    }

    private val configPath = workDir.resolve("user.properties")
    private val userConfig = PropertiesConfiguration()
    private val defaultConfig = PropertiesConfiguration(javaClass.getResource("/config/default.properties"))
    private val resultConfig = CompositeConfiguration()

    init {
        userConfig.file = configPath.toFile()

        resultConfig.addConfiguration(userConfig, true)
        resultConfig.addConfiguration(defaultConfig)
        resultConfig.isDelimiterParsingDisabled = true

        if (Files.exists(configPath)) {
            userConfig.load()
            log.debug("User config loaded from '{}'", configPath)
        } else {
            log.info("User config not found at '{}'", configPath)
        }
    }

    fun get() = resultConfig

    fun save() {
        userConfig.save()
        log.info("User config saved to '{}'", configPath)
    }

}
