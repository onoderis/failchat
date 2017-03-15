package failchat.utils

import org.apache.commons.configuration.PropertiesConfiguration

object Configs

fun loadConfig(): PropertiesConfiguration {
    return PropertiesConfiguration(Configs.javaClass.getResource("/config/default.properties"))
            .apply { load() }
}
