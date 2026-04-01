package feature.agent.ui.list

import feature.agent.domain.Agent
import feature.agent.domain.AgentStatus

data class AgentListState(
    val groups: List<AgentGroup> = emptyList(),
    val slackStatus: SlackStatus = SlackStatus.NotConfigured,
) {
    data class AgentGroup(
        val name: String,
        val agents: List<AgentListEntry>,
    )

    data class AgentListEntry(
        val agent: Agent,
        val status: AgentStatus,
    )

    sealed interface SlackStatus {
        data object NotConfigured : SlackStatus
        data object Connecting : SlackStatus
        data object Connected : SlackStatus
        data object Disconnected : SlackStatus
        data class Error(val message: String) : SlackStatus
    }
}
