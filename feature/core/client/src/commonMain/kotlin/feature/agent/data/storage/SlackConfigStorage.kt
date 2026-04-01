package feature.agent.data.storage

import kotlinx.coroutines.flow.Flow

expect class SlackConfigStorage() {
    fun config(): Flow<SlackConfigEntity?>
    suspend fun save(entity: SlackConfigEntity)
}
