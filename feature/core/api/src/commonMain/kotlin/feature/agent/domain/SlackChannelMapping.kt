package feature.agent.domain

import kotlinx.serialization.Serializable

@Serializable
data class SlackChannelMapping(
    val channelId: String,
    val channelName: String,
    val group: String,
)
