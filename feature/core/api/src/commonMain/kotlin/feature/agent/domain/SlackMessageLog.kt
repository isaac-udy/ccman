package feature.agent.domain

import kotlinx.serialization.Serializable

@Serializable
data class SlackMessageLog(
    val channelId: String,
    val userId: String,
    val text: String,
    val timestamp: String,
)
