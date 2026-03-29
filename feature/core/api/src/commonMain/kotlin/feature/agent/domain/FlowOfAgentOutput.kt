package feature.agent.domain

import kotlinx.coroutines.flow.Flow

fun interface FlowOfAgentOutput {
    operator fun invoke(agentId: Agent.Id): Flow<List<AgentOutput>>
}
