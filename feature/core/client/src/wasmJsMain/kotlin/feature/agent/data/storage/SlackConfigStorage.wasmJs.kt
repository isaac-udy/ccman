package feature.agent.data.storage

import kotlinx.coroutines.flow.Flow

actual class SlackConfigStorage actual constructor() {
    actual fun config(): Flow<SlackConfigEntity?> = throw UnsupportedOperationException("Desktop only")
    actual suspend fun save(entity: SlackConfigEntity): Unit = throw UnsupportedOperationException("Desktop only")
}
