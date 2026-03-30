package feature.agent.data.storage

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File

actual class AgentTaskStorage actual constructor() {

    private val json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
    }

    private val tasksDir = File(System.getProperty("user.home"), ".ccman/tasks").also { it.mkdirs() }

    private val state = MutableStateFlow(loadAllFromDisk())

    actual fun tasksForAgent(agentId: String): Flow<List<AgentTaskEntity>> =
        state.map { all -> all.filter { it.agentId == agentId }.sortedBy { it.startedAt } }

    actual suspend fun save(entity: AgentTaskEntity) {
        val file = File(tasksDir, "${entity.id}.json")
        file.writeText(json.encodeToString(entity))
        state.value = loadAllFromDisk()
    }

    private fun loadAllFromDisk(): List<AgentTaskEntity> {
        if (!tasksDir.exists()) return emptyList()
        return tasksDir.listFiles { file -> file.extension == "json" }
            ?.mapNotNull { file ->
                try {
                    json.decodeFromString<AgentTaskEntity>(file.readText())
                } catch (_: Throwable) {
                    null
                }
            }
            ?: emptyList()
    }
}
