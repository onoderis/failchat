package failchat.util

import org.apache.commons.configuration2.Configuration
import org.apache.commons.configuration2.PropertiesConfiguration
import org.apache.commons.configuration2.builder.FileBasedConfigurationBuilder
import org.apache.commons.configuration2.builder.fluent.Parameters

object Configs

fun loadConfig(): Configuration {
    return FileBasedConfigurationBuilder(PropertiesConfiguration::class.java)
            .configure(
                    Parameters()
                            .properties()
                            .setURL(Configs.javaClass.getResource("/config/default.properties"))
                            .setThrowExceptionOnMissing(true)
            )
            .configuration
}
