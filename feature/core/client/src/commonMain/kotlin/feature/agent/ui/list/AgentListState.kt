package feature.agent.ui.list

import feature.agent.domain.Agent
import feature.agent.domain.AgentStatus

data class AgentListState(
    val agents: List<AgentListEntry> = emptyList(),
) {
    data class AgentListEntry(
        val agent: Agent,
        val status: AgentStatus,
    )
}
