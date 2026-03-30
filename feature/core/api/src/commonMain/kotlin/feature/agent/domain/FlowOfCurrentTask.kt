package feature.agent.domain

import kotlinx.coroutines.flow.Flow

fun interface FlowOfCurrentTask {
    operator fun invoke(agentId: Agent.Id): Flow<AgentTask?>
}
