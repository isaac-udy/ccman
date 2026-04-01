package feature.agent.domain

import kotlinx.serialization.Serializable

@Serializable
data class SlackConfig(
    val botToken: String,
    val appToken: String,
    val enabled: Boolean = false,
    val channelBindings: List<ChannelBinding> = emptyList(),
) {
    @Serializable
    data class ChannelBinding(
        val channelName: String,
        val group: String,
    )
}
