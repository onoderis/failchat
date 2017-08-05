package failchat.core

import org.apache.commons.configuration2.CompositeConfiguration
import org.apache.commons.configuration2.Configuration
import org.apache.commons.configuration2.PropertiesConfiguration
import org.apache.commons.configuration2.builder.FileBasedConfigurationBuilder
import org.apache.commons.configuration2.builder.fluent.Parameters
import org.apache.commons.configuration2.sync.ReadWriteSynchronizer
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.nio.file.Path


/**
 * Загружает и сохраняет конфигурацию.
 * */
class ConfigLoader(workingDirectory: Path) {

    private companion object {
        val log: Logger = LoggerFactory.getLogger(ConfigLoader::class.java)
    }

    private val userConfigPath = workingDirectory.resolve("config/user.properties")
    private val userConfigBuilder = createOptionalConfig(userConfigPath)
    private val defaultConfigBuilder = createMandatoryConfig("/config/default.properties")
    private val privateConfigBuilder = createMandatoryConfig("/config/private.properties")
    private val compositeConfig = CompositeConfiguration()

    init {
        val userConfig = userConfigBuilder.configuration

        // если передавать в конструктор CompositeConfiguration как imMemoryConfig,
        // он будет последний в списке на чтение
        compositeConfig.addConfiguration(userConfig, true)
        compositeConfig.addConfiguration(defaultConfigBuilder)
        compositeConfig.addConfiguration(privateConfigBuilder)
        compositeConfig.synchronizer = ReadWriteSynchronizer()
        compositeConfig.isThrowExceptionOnMissing = true
    }

    fun get(): Configuration = compositeConfig

    fun save() {
        userConfigBuilder.save()
        log.info("User config saved to '{}'", userConfigPath)
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
