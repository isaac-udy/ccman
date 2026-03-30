package feature.agent.ui.detail

import feature.agent.domain.Agent
import feature.agent.domain.AgentOutput
import feature.agent.domain.AgentStatus
import feature.agent.domain.AgentTask

data class AgentDetailState(
    val agent: Agent? = null,
    val status: AgentStatus = AgentStatus.Idle,
    val taskHistory: List<AgentTask> = emptyList(),
    val currentTask: AgentTask? = null,
    val currentPrompt: String = "",
    val showFullOutput: Boolean = false,
)
