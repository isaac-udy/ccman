package feature.agent.domain

import kotlinx.coroutines.flow.Flow

fun interface FlowOfAgentState {
    operator fun invoke(agentId: Agent.Id): Flow<AgentStatus>
}
