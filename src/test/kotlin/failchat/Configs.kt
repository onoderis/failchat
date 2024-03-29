package failchat

import org.apache.commons.configuration2.Configuration
import org.apache.commons.configuration2.PropertiesConfiguration
import org.apache.commons.configuration2.builder.FileBasedConfigurationBuilder
import org.apache.commons.configuration2.builder.fluent.Parameters

object Configs

fun loadDefaultConfig() = loadConfig("/config/default.properties")

private fun loadConfig(resource: String): Configuration {
    return FileBasedConfigurationBuilder(PropertiesConfiguration::class.java)
            .configure(
                    Parameters()
                            .properties()
                            .setURL(Configs.javaClass.getResource(resource))
                            .setThrowExceptionOnMissing(true)
            )
            .configuration
}
