package feature.agent.data.storage

import kotlinx.serialization.Serializable

@Serializable
data class AgentConfigEntity(
    val id: String,
    val name: String,
    val workingDirectory: String,
    val instructions: String = "",
)
