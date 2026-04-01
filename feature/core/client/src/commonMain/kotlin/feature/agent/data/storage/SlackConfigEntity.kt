package feature.agent.data.storage

import kotlinx.serialization.Serializable

@Serializable
data class SlackConfigEntity(
    val botToken: String,
    val appToken: String,
    val enabled: Boolean = false,
)
