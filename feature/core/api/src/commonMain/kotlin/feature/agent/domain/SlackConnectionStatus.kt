package feature.agent.domain

import kotlinx.serialization.Serializable

@Serializable
sealed interface SlackConnectionStatus {
    @Serializable
    data object Disconnected : SlackConnectionStatus

    @Serializable
    data object Connecting : SlackConnectionStatus

    @Serializable
    data object Connected : SlackConnectionStatus

    @Serializable
    data class Error(val message: String) : SlackConnectionStatus
}
