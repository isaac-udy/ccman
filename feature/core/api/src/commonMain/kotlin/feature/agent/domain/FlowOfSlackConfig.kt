package feature.agent.domain

import kotlinx.coroutines.flow.Flow

fun interface FlowOfSlackConfig {
    operator fun invoke(): Flow<SlackConfig?>
}
