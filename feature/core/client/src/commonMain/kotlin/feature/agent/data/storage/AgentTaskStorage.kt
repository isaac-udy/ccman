package feature.agent.data.storage

import kotlinx.coroutines.flow.Flow

expect class AgentTaskStorage() {
    fun tasksForAgent(agentId: String): Flow<List<AgentTaskEntity>>
    suspend fun save(entity: AgentTaskEntity)
}
