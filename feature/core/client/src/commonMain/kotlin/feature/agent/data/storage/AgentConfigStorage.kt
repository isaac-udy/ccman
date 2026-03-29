package feature.agent.data.storage

import kotlinx.coroutines.flow.Flow

expect class AgentConfigStorage() {
    fun agents(): Flow<List<AgentConfigEntity>>
    suspend fun save(entity: AgentConfigEntity)
    suspend fun delete(id: String)
}
