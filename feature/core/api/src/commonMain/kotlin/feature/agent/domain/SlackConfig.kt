package feature.agent.domain

import kotlinx.serialization.Serializable

@Serializable
data class SlackConfig(
    val botToken: String,
    val appToken: String,
    val enabled: Boolean = false,
)
