package feature.agent.domain

import kotlinx.coroutines.flow.Flow

fun interface FlowOfAgents {
    operator fun invoke(): Flow<List<Agent>>
}
