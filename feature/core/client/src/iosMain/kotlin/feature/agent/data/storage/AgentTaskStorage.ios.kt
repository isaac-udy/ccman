package feature.agent.data.storage

import kotlinx.coroutines.flow.Flow

actual class AgentTaskStorage actual constructor() {
    actual fun tasksForAgent(agentId: String): Flow<List<AgentTaskEntity>> = throw UnsupportedOperationException("Desktop only")
    actual suspend fun save(entity: AgentTaskEntity): Unit = throw UnsupportedOperationException("Desktop only")
}
