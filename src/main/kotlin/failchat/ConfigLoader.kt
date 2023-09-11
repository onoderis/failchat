package failchat

import mu.KotlinLogging
import org.apache.commons.configuration2.CompositeConfiguration
import org.apache.commons.configuration2.Configuration
import org.apache.commons.configuration2.PropertiesConfiguration
import org.apache.commons.configuration2.builder.FileBasedConfigurationBuilder
import org.apache.commons.configuration2.builder.fluent.Parameters
import org.apache.commons.configuration2.sync.ReadWriteSynchronizer
import java.nio.file.Files
import java.nio.file.Path


/**
 * Загружает и сохраняет конфигурацию.
 * */
class ConfigLoader(private val configDirectory: Path) {

    private companion object {
        val logger = KotlinLogging.logger {}
    }

    private val userConfigPath = configDirectory.resolve("user.properties")
    private val defaultConfig = createMandatoryConfig("/config/default.properties")
    private val privateConfig = createMandatoryConfig("/config/private.properties")

    @Volatile
    private var loadedConfig: LoadedConfig? = null

    fun load(): Configuration {
        loadedConfig?.let { return it.compositeConfig }

        val userConfigBuilder = createOptionalConfig(userConfigPath)
        val userConfig = userConfigBuilder.configuration

        // Если передавать в конструктор CompositeConfiguration как inMemoryConfig,
        // он будет последний в списке на чтение
        val compositeConfig = CompositeConfiguration()

        compositeConfig.addConfiguration(userConfig, true)
        compositeConfig.addConfiguration(defaultConfig)
        compositeConfig.addConfiguration(privateConfig)
        compositeConfig.synchronizer = ReadWriteSynchronizer()
        compositeConfig.isThrowExceptionOnMissing = true

        loadedConfig = LoadedConfig(userConfigBuilder, compositeConfig)

        return compositeConfig
    }

    fun dropLoadedConfig() {
        loadedConfig = null
        logger.info("Loaded config was dropped")
    }

    fun save() {
        Files.createDirectories(configDirectory)
        val config = loadedConfig ?: run {
            logger.warn("There is not last loaded config to save")
            return
        }

        config.userConfigBuilder.save()
        logger.info("User config saved to '{}'", userConfigPath)
    }

    fun deleteUserConfigFile() {
        Files.deleteIfExists(userConfigPath)
        logger.info("User configuration file was deleted, path: {}", userConfigPath)
    }

    private fun createOptionalConfig(path: Path): FileBasedConfigurationBuilder<PropertiesConfiguration> {
        // last argument (true) - do not throw exception if config not exists, just get empty config
        return FileBasedConfigurationBuilder(PropertiesConfiguration::class.java, null, true)
                .configure(
                        Parameters()
                                .properties()
                                .setPath(path.toAbsolutePath().toString())
                                .setThrowExceptionOnMissing(true)
                )
    }

    private fun createMandatoryConfig(resource: String): PropertiesConfiguration {
        return FileBasedConfigurationBuilder(PropertiesConfiguration::class.java)
                .configure(
                        Parameters()
                                .properties()
                                .setURL(javaClass.getResource(resource))
                                .setThrowExceptionOnMissing(true)
                )
                .configuration
    }

    private class LoadedConfig(
            val userConfigBuilder: FileBasedConfigurationBuilder<PropertiesConfiguration>,
            val compositeConfig: CompositeConfiguration
    )
}
