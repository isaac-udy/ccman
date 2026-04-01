package feature.agent.data.storage

import kotlinx.coroutines.flow.Flow

data class SlackMessageEntity(
    val channelId: String,
    val threadTs: String?,
    val messageTs: String,
    val text: String,
    val userId: String,
)

data class SlackChannelEntity(
    val id: String,
    val name: String,
)

expect class SlackServiceStorage() {
    fun connect(botToken: String, appToken: String): Flow<SlackMessageEntity>
    fun disconnect()
    fun isConnected(): Boolean
    suspend fun postMessage(channelId: String, text: String, threadTs: String?): String
    suspend fun updateMessage(channelId: String, ts: String, text: String)
    suspend fun listChannels(): List<SlackChannelEntity>
    suspend fun createChannel(name: String): SlackChannelEntity
}
