package feature.agent.domain

import kotlinx.serialization.Serializable

@Serializable
data class SlackThreadSession(
    val threadTs: String,
    val channelId: String,
    val sessionId: String,
    val agentId: Agent.Id,
    val branch: String,
    val lastUpdated: String,
)
