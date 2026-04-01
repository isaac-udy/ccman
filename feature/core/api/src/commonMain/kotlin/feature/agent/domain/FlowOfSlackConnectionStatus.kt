package feature.agent.domain

import kotlinx.coroutines.flow.Flow

fun interface FlowOfSlackConnectionStatus {
    operator fun invoke(): Flow<SlackConnectionStatus>
}
