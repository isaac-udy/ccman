package feature.agent.data.storage

import kotlinx.serialization.Serializable

@Serializable
data class SlackThreadSessionEntity(
    val threadTs: String,
    val channelId: String,
    val sessionId: String,
    val agentId: String,
    val branch: String,
    val lastUpdated: String,
)
