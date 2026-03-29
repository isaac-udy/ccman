package feature.agent.domain

import kotlinx.serialization.Serializable

@Serializable
sealed interface AgentStatus {
    @Serializable
    data object Idle : AgentStatus

    @Serializable
    data object Running : AgentStatus

    @Serializable
    data class Error(val message: String) : AgentStatus
}
