package failchat

import mu.KLogging
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

    private companion object : KLogging()

    private val userConfigPath = configDirectory.resolve("user.properties")
    private val userConfigBuilder = createOptionalConfig(userConfigPath)
    private val userConfig = userConfigBuilder.configuration
    private val defaultConfig = createMandatoryConfig("/config/default.properties")
    private val privateConfig = createMandatoryConfig("/config/private.properties")
    private val compositeConfig = CompositeConfiguration()

    init {
        // Если передавать в конструктор CompositeConfiguration как inMemoryConfig,
        // он будет последний в списке на чтение
        compositeConfig.addConfiguration(userConfig, true)
        compositeConfig.addConfiguration(defaultConfig)
        compositeConfig.addConfiguration(privateConfig)
        compositeConfig.synchronizer = ReadWriteSynchronizer()
        compositeConfig.isThrowExceptionOnMissing = true
    }

    fun get(): Configuration = compositeConfig

    fun save() {
        Files.createDirectories(configDirectory)
        userConfigBuilder.save()
        logger.info("User config saved to '{}'", userConfigPath)
    }

    fun resetConfigurableByUserProperties() {
        ConfigKeys.configurableByUserProperties.forEach {
            userConfig.clearProperty(it)
        }
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

}
