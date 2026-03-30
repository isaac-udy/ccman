package feature.agent.domain

import kotlinx.coroutines.flow.Flow

fun interface FlowOfAgentTasks {
    operator fun invoke(agentId: Agent.Id): Flow<List<AgentTask>>
}
