package feature.agent.domain

import kotlinx.coroutines.flow.Flow

fun interface FlowOfSlackMessages {
    operator fun invoke(): Flow<List<SlackMessageLog>>
}
