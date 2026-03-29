package feature.agent.data.storage

import kotlinx.coroutines.flow.Flow

actual class AgentConfigStorage actual constructor() {
    actual fun agents(): Flow<List<AgentConfigEntity>> = throw UnsupportedOperationException("Desktop only")
    actual suspend fun save(entity: AgentConfigEntity): Unit = throw UnsupportedOperationException("Desktop only")
    actual suspend fun delete(id: String): Unit = throw UnsupportedOperationException("Desktop only")
}
