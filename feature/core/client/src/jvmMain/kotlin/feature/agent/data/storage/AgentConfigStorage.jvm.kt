package feature.agent.data.storage

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File

actual class AgentConfigStorage actual constructor() {

    private val json = Json { prettyPrint = true }

    private val configDir = File(System.getProperty("user.home"), ".ccman").also { it.mkdirs() }
    private val configFile = File(configDir, "agents.json")

    private val state = MutableStateFlow(loadFromDisk())

    actual fun agents(): Flow<List<AgentConfigEntity>> = state

    actual suspend fun save(entity: AgentConfigEntity) {
        val current = state.value.toMutableList()
        val index = current.indexOfFirst { it.id == entity.id }
        if (index >= 0) {
            current[index] = entity
        } else {
            current.add(entity)
        }
        state.value = current
        saveToDisk(current)
    }

    actual suspend fun delete(id: String) {
        val current = state.value.filterNot { it.id == id }
        state.value = current
        saveToDisk(current)
    }

    private fun loadFromDisk(): List<AgentConfigEntity> {
        if (!configFile.exists()) return emptyList()
        return try {
            json.decodeFromString<List<AgentConfigEntity>>(configFile.readText())
        } catch (_: Throwable) {
            emptyList()
        }
    }

    private fun saveToDisk(entities: List<AgentConfigEntity>) {
        configFile.writeText(json.encodeToString(entities))
    }
}
