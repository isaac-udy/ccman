package feature.agent.data.storage

import kotlinx.coroutines.flow.Flow

actual class SlackServiceStorage actual constructor() {
    actual fun connect(botToken: String, appToken: String): Flow<SlackMessageEntity> = throw UnsupportedOperationException("Desktop only")
    actual fun disconnect(): Unit = throw UnsupportedOperationException("Desktop only")
    actual fun isConnected(): Boolean = throw UnsupportedOperationException("Desktop only")
    actual suspend fun postMessage(channelId: String, text: String, threadTs: String?): String = throw UnsupportedOperationException("Desktop only")
    actual suspend fun updateMessage(channelId: String, ts: String, text: String): Unit = throw UnsupportedOperationException("Desktop only")
    actual suspend fun uploadFile(channelId: String, threadTs: String?, filename: String, content: String, initialComment: String?): Unit = throw UnsupportedOperationException("Desktop only")
    actual suspend fun listChannels(): List<SlackChannelEntity> = throw UnsupportedOperationException("Desktop only")
    actual suspend fun createChannel(name: String): SlackChannelEntity = throw UnsupportedOperationException("Desktop only")
}
