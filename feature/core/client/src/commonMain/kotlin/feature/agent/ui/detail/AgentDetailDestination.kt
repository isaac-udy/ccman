package feature.agent.ui.detail

import dev.enro.NavigationKey
import kotlinx.serialization.Serializable

@Serializable
data class AgentDetailDestination(
    val agentId: String,
) : NavigationKey
