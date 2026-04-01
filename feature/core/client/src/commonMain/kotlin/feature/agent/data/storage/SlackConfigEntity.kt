package feature.agent.data.storage

import kotlinx.serialization.Serializable

@Serializable
data class SlackConfigEntity(
    val botToken: String,
    val appToken: String,
    val enabled: Boolean = false,
    val channelBindings: List<ChannelBindingEntity> = emptyList(),
)

@Serializable
data class ChannelBindingEntity(
    val channelName: String,
    val group: String,
)
