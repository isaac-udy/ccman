package feature.agent.ui.edit

import dev.enro.NavigationKey
import kotlinx.serialization.Serializable

@Serializable
data class AgentEditDestination(
    val agentId: String?,
) : NavigationKey
