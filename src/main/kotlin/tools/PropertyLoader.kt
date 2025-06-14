package tools

import java.io.FileInputStream
import java.util.Properties
import java.util.logging.Logger

class PropertyLoader {
    companion object {
        private val logger = Logger.getLogger(PropertyLoader::class.java.name)
        private val properties = Properties()

        init {
            val localPropertiesFile = FileInputStream("local.properties")
            properties.load(localPropertiesFile)
            localPropertiesFile.close()
        }

        fun getProperty(key: String): String {
            return properties.getProperty(key) ?: throw IllegalStateException("Property '$key' not found in local.properties")
        }
    }
}
