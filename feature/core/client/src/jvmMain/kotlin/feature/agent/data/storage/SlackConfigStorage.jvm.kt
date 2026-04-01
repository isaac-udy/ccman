package feature.agent.data.storage

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File

actual class SlackConfigStorage actual constructor() {

    private val json = Json { prettyPrint = true }

    private val configDir = File(System.getProperty("user.home"), ".ccman").also { it.mkdirs() }
    private val configFile = File(configDir, "slack-config.json")

    private val state = MutableStateFlow(loadFromDisk())

    actual fun config(): Flow<SlackConfigEntity?> = state

    actual suspend fun save(entity: SlackConfigEntity) {
        configFile.writeText(json.encodeToString(entity))
        state.value = entity
    }

    private fun loadFromDisk(): SlackConfigEntity? {
        if (!configFile.exists()) return null
        return try {
            json.decodeFromString<SlackConfigEntity>(configFile.readText())
        } catch (_: Throwable) {
            null
        }
    }
}
