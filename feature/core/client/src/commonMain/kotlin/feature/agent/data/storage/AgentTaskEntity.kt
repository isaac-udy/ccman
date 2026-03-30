package feature.agent.data.storage

import feature.agent.domain.AgentOutput
import kotlinx.serialization.Serializable

@Serializable
data class AgentTaskEntity(
    val id: String,
    val agentId: String,
    val prompt: String,
    val output: List<AgentOutput>,
    val status: String,
    val startedAt: String,
    val completedAt: String?,
)
