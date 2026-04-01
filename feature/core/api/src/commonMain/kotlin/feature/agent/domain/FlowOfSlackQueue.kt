package feature.agent.domain

import kotlinx.coroutines.flow.Flow

fun interface FlowOfSlackQueue {
    operator fun invoke(): Flow<List<SlackQueueEntry>>
}
